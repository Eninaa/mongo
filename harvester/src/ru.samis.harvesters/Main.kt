package ru.samis.harvesters

import ru.samis.harvesters.lands.LandHarvester
import ru.samis.harvesters.oks.OksHarvester
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import ru.samis.harvesters.configChecker.ConfigChecker
import ru.samis.harvesters.land.IngeoHarvester
import ru.samis.harvesters.land.PenzaHarvester
import java.io.File
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    var time = -System.nanoTime()


    val configChecker = ConfigChecker()
// configChecker.checkBaseConfig()
//configChecker.checkAddressConfig()

    if (args.isEmpty()) return
    when (args[0]) {
        "xy" -> XYCalculator()
        "centroid" -> CentroidCalculator()
        "msprep" -> MsPrepare()
        "ms" -> MsHarvester()
        "penza" -> PenzaHarvester()
        "ingeo" -> IngeoHarvester()

        "2gis", "2gis_csv" -> DGISCSVHarvester()

        "reformagkh" -> ReformaGKHharvester()
//      "fias" -> FiasParser()
        "fias", "gar" -> GarParser()

        "gar_streets" -> GarStreetsParser()

        "gar_settlements" -> GarSettlementsParser()

        "osm" -> OsmParserJson()

        "uiks" -> CikParser()

        "moscow" -> MoscowHarvester()

        "lands" -> {
            configChecker.checkBaseConfig()
            val fileType = JSONObject(File("settings.json").readText()).getJSONObject("params")?.getString("fileType")
            try {
                val cls = Class.forName("ru.samis.harvesters.lands.LandHarvester$fileType")
                cls.newInstance() as LandHarvester
            } catch (e: Exception) {
                null
            }
        }


        "oks" -> {
            configChecker.checkBaseConfig()
            val fileType = JSONObject(File("settings.json").readText()).getJSONObject("params")?.getString("fileType")
            try {
                val cls = Class.forName("ru.samis.harvesters.oks.OksHarvester$fileType")
                println(cls)
                cls.newInstance() as OksHarvester
            } catch (e: Exception) {
                null
            }
        }
        "yandex" -> YandexHarvester()
        else -> null
    }?.apply {
        harvest()
        close()
    }

    time += System.nanoTime()
    println("time: ${(time / 1e6).toInt() / 1000} seconds")
    exitProcess(0)
}

fun settlements() {
    val settings = JSONObject(File("settings.json").readText())
    val params = settings.getJSONObject("params")

    val client = MongoClients
        .create(params.getString("ConnectionString"))
    val db: MongoDatabase = client
        .getDatabase("rk_common")
    val housesCollection = db.getCollection("settlement")

    val all = housesCollection.find()
        .projection(Document("name", 1).append("region", 1).append("municipalitet", 1))
        .toList()
//        .sortedBy { doc -> doc.getString("name") }

    val byNames = mutableMapOf<String, MutableList<Document>>()
//    val extInfo = mutableMapOf<String, Document>()
    for (settl in all) {
//        println(settl)
        val name = settl.getString("name") ?: continue
        if (name.toIntOrNull() != null) continue
        val list = byNames.getOrPut(name, { mutableListOf() })
        list += settl
    }
    val nouns = byNames.filter { entry ->
        if (entry.key.length < 3) return@filter false
        if (entry.key[0].isDigit()) return@filter false
        if (entry.key.endsWith("ая")) return@filter false
        if (entry.key.endsWith("яя")) return@filter false
        if (entry.key.endsWith("ий")) return@filter false
        if (entry.key.endsWith("ой")) return@filter false
        if (entry.key.endsWith("ого")) return@filter false
        if (entry.key.endsWith("ый")) return@filter false
        if (entry.key.endsWith("ин")) return@filter false
        if (entry.key.endsWith("ина")) return@filter false
        if (entry.key.endsWith("ии")) return@filter false
        if (entry.key.endsWith("ов")) return@filter false
        if (entry.key.endsWith("ова")) return@filter false
        if (entry.key.endsWith("ева")) return@filter false
        if (entry.key.endsWith("ев")) return@filter false
        if (entry.key.endsWith("ёв")) return@filter false
        if (entry.key.endsWith("ёва")) return@filter false
        if (entry.key.toLowerCase().contains("октября")) return@filter false
        if (entry.key.toLowerCase().contains("линия")) return@filter false
        if (entry.key.toLowerCase().contains("городок")) return@filter false
        if (entry.key.toLowerCase().contains(" либкнехта")) return@filter false
        if (entry.key.toLowerCase() == "саха") return@filter false
        if (entry.key.toLowerCase() == "учхоза") return@filter false
        if (entry.key.toLowerCase() == "нелазское") return@filter false
        if (entry.key.toLowerCase() == "большие полянки") return@filter false
        if (entry.key.toLowerCase() == "люксембург") return@filter false
        if (entry.key.toLowerCase() == "башкортостан") return@filter false
        if (entry.key.toLowerCase() == "москва") return@filter false
        if (entry.key.toLowerCase() == "санкт-петербург") return@filter false
        if (entry.key.toLowerCase() == "№ 20") return@filter false
        if (entry.key.toLowerCase() == "свободы") return@filter false
        if (entry.key.toLowerCase() == "щель") return@filter false
        if (entry.key.toLowerCase() == "алтай") return@filter false
        if (entry.key.toLowerCase() == "поселок") return@filter false
        if (entry.key.toLowerCase() == "посёлок") return@filter false
        if (entry.key.toLowerCase() == "верх") return@filter false
        if (entry.key.toLowerCase() == "почта") return@filter false
        if (entry.key.toLowerCase() == "югра") return@filter false
        if (entry.key.toLowerCase() == "герцена") return@filter false
        if (entry.key.toLowerCase() == "северо") return@filter false
        if (entry.key.toLowerCase() == "разина") return@filter false
        if (entry.key.toLowerCase() == "серебряно") return@filter false
        if (entry.key.toLowerCase() == "славы") return@filter false
        if (entry.key.toLowerCase() == "энгельса") return@filter false
        if (entry.key.toLowerCase() == "гастелло") return@filter false
        if (entry.key.toLowerCase() == "дача") return@filter false
        if (entry.key.toLowerCase() == "строение") return@filter false
        if (entry.key.toLowerCase() == "улица") return@filter false
        if (entry.key.toLowerCase() == "ком") return@filter false
        if (entry.key.toLowerCase() == "левый берег") return@filter false
        if (entry.key.toLowerCase() == "парижской коммуны") return@filter false
        true
    }.toMutableMap()

//    val includers = mutableListOf<String>()
//    val nounsList = nouns.keys.toList()
//    for (i in 0 until nounsList.size) {
//        val name1 = nounsList[i]
//
//        for (j in i + 1 until nounsList.size) {
//            val name2 = nounsList[j]
//            if (name2.contains(name1)) {
//                includers += name1
//            } else if (name1.contains(name2)) {
//                includers += name2
//            }
//        }
//        if (i % 1000 == 0) println("$i / ${nounsList.size}")
//    }
//    for (includer in includers) {
//        nouns.remove(includer)
//    }

    val sorted = nouns.toList().toMutableList()
    sorted.sortWith { pair1, pair2 ->
        pair2.first.length.compareTo(pair1.first.length)
    }

    File("settlements.jsonl").bufferedWriter().use { writer ->
        for (group in sorted) {
            val regions = hashSetOf<String>()
            val doublesRegions = hashSetOf<String>()
            for (document in group.second) {
                val region = document.getString("region") ?: continue
                if (regions.contains(region))
                    doublesRegions += region
                else
                    regions += region
            }
            for (double in doublesRegions) {
                regions.remove(double)
            }
            with(JSONObject()) {
                put("name", group.first)
                put("regions", JSONArray().apply {
                    for (region in regions) {
                        put(region)
                    }
                })
                writer.write(toString())
                writer.write("\n")
            }

//        println(key)
        }
    }

    client.close()
}




