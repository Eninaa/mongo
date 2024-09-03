package ru.samis.harvesters

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoNamespace
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates
import org.bson.Document
import org.bson.UuidRepresentation
import org.bson.types.ObjectId
import org.json.JSONArray
import org.json.JSONObject
import org.locationtech.jts.io.geojson.GeoJsonReader
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

abstract class Harvester : AutoCloseable {
    private val errors = mutableListOf<String>()
    private var progress = 0.0
    private var count = 0
    private var completed = false
    protected val settings by lazy { JSONObject(File("settings.json").readText()) }
    protected val params by lazy { settings.getJSONObject("params") }
    protected val options by lazy { settings.getJSONObject("options") }
    protected val regionCode by lazy { params.optString("RegionCode") }
    protected val client by lazy {
        MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(params.getString("ConnectionString")))
                .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
                .build()
        )
    }
    protected val db by lazy { client.getDatabase(params.getString("database")) }
    protected open val housesCollection by lazy { db.getCollection("${params.getString("dataset")}_inProgress") }

    //
    protected val updateCollections by lazy {  params.optBoolean("updateCollections", true)}

    override fun close() {
        client.close()
    }

    protected fun writeComplete(count: Int) {
        completed = true
        writeProgress(1.0, count)
    }

    protected fun writeProgress(progress: Double, count: Int) {
        this.progress = progress
        this.count = count
        flush()
    }

    protected fun incCount(countInc: Int = 1) {
        this.count += countInc
        flush()
    }

    protected fun writeError(descr: String) {
        errors += "\n" + descr
        flush()
    }

    private var countBefore = 0L
    private var countAfter = 0L
    private var countNew = 0
    private var countDelete = 0
    private var countChange = 0
    private var ratioNew = 0.0
    private var ratioDelete = 0.0
    private var ratioChange = 0.0
    private val fullness = JSONObject()

    private fun flush() {
        val err = JSONArray()
        for (error in errors) {
            err.put(error)
        }
        FileWriter("info.json").use {
            it.write(
                JSONObject()
                    .put("progress", progress)
                    .put("inserted", count)
                    .put("errors", err)
                    .put("completed", completed)
                    .put("countBefore", countBefore)
                    .put("countAfter", countAfter)
                    .put("countNew", countNew)
                    .put("countDelete", countDelete)
                    .put("countChange", countChange)
                    .put("ratioNew", ratioNew)
                    .put("ratioDelete", ratioDelete)
                    .put("ratioChange", ratioChange)
                    .apply { if (fullness.keySet().isNotEmpty()) put("fullness", fullness) }
                    .toString()
            )
        }
    }


    private fun preHarvest() {
        val oldColl = db.getCollection(params.getString("dataset"))
        countBefore = oldColl.countDocuments()
    }

    private fun compareVersions(oldDoc: Document): Document? {
        // если по какой-то причине в найденном доке нет вложенного нового объекта, то какой смысл возвращать старый док?
        val new = (oldDoc["new"] as Document?) ?: return oldDoc

        val toRemove = hashSetOf<String>()
        for ((key, value) in new) {
            if (key == "ID" || key == "_id") continue
            if (value == oldDoc[key]) {
                oldDoc.remove(key)
                toRemove += key
            }
        }

        for (key in toRemove) {
            new.remove(key)
        }
        new.remove("_id")

        if (oldDoc.size == 3) return null

        return oldDoc
    }

    private fun postHarvest() {

        countAfter = housesCollection.countDocuments()
        // TODO
        computeDelta()
        computeFullness()

        val collName = params.getString("dataset")
        val oldName = "${collName}_${SimpleDateFormat("yyyy_MM_dd_HH_mm").format(Date())}"


        // existing -> delta
        //TODO
        // добавила if - на новых коллекциях падает - или из-за багов библиотеки падает
        if (db.listCollectionNames().contains(collName)) {
            db.getCollection(collName).renameCollection(
                MongoNamespace(params.getString("database"), oldName)
            )
        }
        // inProgress -> _houses
        housesCollection.renameCollection(
            MongoNamespace(params.getString("database"), collName)
        )
        flush()
    }

    private fun computeFullness() {
        if (options != null && !options.isEmpty) {
            val fullness = options.optJSONArray("fullness") ?: return
            for (i in 0 until fullness.length()) {
                val field = fullness.getString(i)

                val notNull = housesCollection.find(Filters.ne(field, null))
                var count = 0
                for (doc in notNull) {
                    if (doc[field]?.toString()?.isBlank() == false) {
                        count++
                    }
                }
                this.fullness.put(field, count)
                this.fullness.put("${field}_ratio", 1.0 * count / countAfter)
                flush()
            }
        }
    }

    private fun computeDelta() {
        val oldColl = db.getCollection(params.getString("dataset"))
//        if (oldColl.countDocuments() == 0L) return

        val collName = params.getString("dataset")

        // поиск новых документов (по ID) из целевой коллекции и inProgress
        // нет совпадающих ID в тек и целевой коллекциях
        val added = housesCollection.aggregate(
            mutableListOf(
                Aggregates.lookup(collName, "ID", "ID", "old"),
                Aggregates.match(Filters.size("old", 0))
            )
        )

        countNew = added.count()
        if (countBefore > 0)
            ratioNew = 1.0 * countNew / countBefore
        println("added $countNew")
        flush()

        // все найденные свежие документы добавляем в дельту коллекцию с тегом todo = add
        for (newDoc in added) {
            newDoc["todo"] = "add"
            newDoc.remove("old")
            try {
                oldColl.insertOne(newDoc)
            } catch (e: Exception) {
                println("Failed to add new document to delta collection:  + ${newDoc["_id"]} + $e.message")
            }
        }

        val intersection = oldColl.aggregate(
            mutableListOf(
                Aggregates.lookup(collName + "_inProgress", "ID", "ID", "new"),
                Aggregates.match(Filters.eq("todo", null)),
                Aggregates.unwind("\$new")
            )
        )

        println("matched ids ${intersection.count()}")

        countChange = 0
        var equalCount = 0
        for (oldDoc in intersection) {
            val compareResult = compareVersions(oldDoc)
            if (compareResult == null) {
                oldColl.deleteOne(Filters.eq("ID", oldDoc["ID"]))
                equalCount++
            } else {
                compareResult["todo"] = "change"
                val id = compareResult["_id"]
                try {
                    oldColl.replaceOne(
                        Filters.eq("_id", oldDoc["_id"] as ObjectId),
                        compareResult
                    )
                    countChange++
                } catch (e: Exception) {
                    println("Failed to replace changed document in delta collection:  + $id + $e.message");
                }
            }
        }
        println("changed $countChange")
        if (countBefore > 0)
            ratioChange = 1.0 * countChange / countBefore
        println("equal $equalCount")
        flush()

        val deleted = oldColl.find(Filters.eq("todo", null)).projection(Document("_id", 1))
        countDelete = deleted.count()
        println("deleted $countDelete")

        if (countBefore > 0)
            ratioDelete = 1.0 * countDelete / countBefore
        flush()
        oldColl.updateMany(
            Filters.eq("todo", null),
            Updates.set("todo", "delete")
        )
    }

    fun harvest() {
        preHarvest()
        val inserted = mainHarvest()

        // TODO
        if (updateCollections) postHarvest()

        if (inserted > 0)
            writeComplete(inserted)


    }

    @kotlin.jvm.Throws(IOException::class, InterruptedException::class)
    protected abstract fun mainHarvest(): Int

    protected fun insertMetadata(datasetStructure: Array<Array<String>>) {
        val dbName = params.getString("database")
        val dataset = params.getString("dataset")
        MongoClients.create(params.getString("ConnectionString"))
            .getDatabase("rk_metadata")
            .getCollection("datasetsStructure").apply {
                Document("database", dbName).append("dataset", dataset).apply {
                    append("fields", List(datasetStructure.size) { i ->
                        val record = datasetStructure[i]
                        Document().append("name", record[0]).append("caption", record[1]).append("type", record[2])
                    })
                    replaceOne(
                        Filters.and(
                            Filters.eq("database", dbName),
                            Filters.eq("dataset", dataset)
                        ),
                        this,
                        ReplaceOptions().upsert(true)
                    )
                }
            }
    }

    companion object {
        fun trueGeometry(geometry: List<*>?): Boolean {
            geometry ?: return false
            if (geometry.isEmpty()) return false
            if (geometry[0] is Number) return true
            if (geometry[0] !is List<*>) return false

            var result = true

            if ((geometry[0] as List<*>)[0] is List<*>) {
                for (i in geometry.indices) {
                    val item = geometry[0] as List<*>
                    result = trueGeometry(item)
                    if (!result) return false
                }
                return true
            }

            val vector = geometry.map { list -> list as List<Double> }
            val isPoly = vector[0][0] == vector.last()[0] && vector[0][1] == vector.last()[1]
            val offset = if (isPoly) 1 else 0
            for (i in geometry.indices) {
                val point1 = vector[i]
                val point1merc = listOf(lat2merc(point1[1]), lon2merc(point1[0]))

                val prevIndex = if (i == 0) vector.lastIndex - offset else i - 1
                val point0 = vector[prevIndex]
                val point0merc = listOf(lat2merc(point0[1]), lon2merc(point0[0]))

                val nextIndex = if (i < vector.lastIndex) i + 1 else offset
                val point2 = vector[nextIndex]
                val point2merc = listOf(lat2merc(point2[1]), lon2merc(point2[0]))

                val v1x = point1merc[1] - point0merc[1]
                val v1y = point1merc[0] - point0merc[0]

                val v2x = point2merc[1] - point1merc[1]
                val v2y = point2merc[0] - point1merc[0]

                val angle = angleBetweenVectors(v1x, v1y, v2x, v2y) / PI * 180
                if ((angle > 5 && angle < 85) || (angle > 95 && angle < 175)) return false
            }

            return true
        }

        // Return the smaller of the two angles between two 2D vectors in radians
        fun angleBetweenVectors(v1x: Double, v1y: Double, v2x: Double, v2y: Double): Double {

            // To determine the angle between two vectors v1 and v2 we can use
            // the following formula: dot(v1,v2) = len(v1)*len(v2)*cosθ and solve
            // for θ where dot(a,b) is the dot product and len(c) is the length of c.
            val dotproduct = v1x * v2x + v1y * v2y
            val v1Length = sqrt(v1x * v1x + v1y * v1y)
            val v2Length = sqrt(v2x * v2x + v2y * v2y)

            val value = dotproduct / (v1Length * v2Length)

            // Double value rounding precision may lead to the value we're about to pass into
            // the arccos function to be slightly outside its domain, so do a safety check.
            if (value <= -1.0) return PI
            return if (value >= +1.0) 0.0 else acos(value)
        }

        fun centroid(geoJson: String) =
            GeoJsonReader().read(geoJson).centroid


        fun avgRaduis(
            points: List<*>,
            centerX: Double,
            centerY: Double,
            radiuses: Array<Double> = arrayOf(0.0),
            count: Array<Int> = arrayOf(0)
        ): Double {
            if (points[0] is List<*> && (points[0] as List<*>)[0] is Double) {
                for (point in points) {
                    val point = point as List<Double>
                    val y = lat2merc(point[1])
                    val x = lon2merc(point[0])
                    radiuses[0] += sqrt((x - centerX).pow(2.0) + (y - centerY).pow(2.0))
                    count[0]++
                }
            } else if (points[0] is Double) {
                val point = points as List<Double>
                val x = lat2merc(point[1])
                val y = lon2merc(point[0])
                radiuses[0] += sqrt((x - centerX).pow(2.0) + (y - centerY).pow(2.0))
                count[0]++
            } else {
                for (point in points) {
                    avgRaduis(point as List<*>, centerX, centerY, radiuses, count)
                }

            }
            return if (count[0] == 0) 0.0 else radiuses[0] / count[0]
        }

        fun lat2merc(lat: Double) =
            3189068.5 * ln((1.0 + sin(lat * 0.017453292519943295)) / (1.0 - sin(lat * 0.017453292519943295)))

        fun lon2merc(lon: Double) = lon * 0.017453292519943295 * 6378137.0
    }
}