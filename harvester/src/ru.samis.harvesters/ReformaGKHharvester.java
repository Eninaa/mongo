package ru.samis.harvesters;

import com.google.common.io.CharStreams;
import com.google.gson.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.apache.commons.codec.Charsets;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bson.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;


public class ReformaGKHharvester extends Harvester {
    private static String settlementURL = "https://dom.gosuslugi.ru/nsi/api/rest/services/nsi/fias/v4/settlements?actual=true";
    private static String cityURL = "https://dom.gosuslugi.ru/nsi/api/rest/services/nsi/fias/v4/cities?actual=true";
    private static String areaURL = "https://dom.gosuslugi.ru/nsi/api/rest/services/nsi/fias/v4/areas?actual=true";
    private static String streetURL = "https://dom.gosuslugi.ru/nsi/api/rest/services/nsi/fias/v4/streets?actual=true";
    private static String elementURL = "https://dom.gosuslugi.ru/nsi/api/rest/services/nsi/fias/v4/planning/structure/elements?actual=true";

    private static String POST(String url, String body) throws IOException {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Host,Content-Length,Connection");
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(url);
        request.addHeader("Host", "dom.gosuslugi.ru");
        request.addHeader("State-Guid", "/houses");
        request.addHeader("Sec-Ch-Ua", "\"Chromium\";v=\"123\", \"Not:A-Brand\";v=\"8\"");
        request.addHeader("Sec-Ch-Ua-Platform", "Windows");
        request.addHeader("Sec-Ch-Ua-Mobile", "?0");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.122 Safari/537.36");
        request.addHeader("Content-Type", "application/json;charset=UTF-8");
        request.addHeader("Session-Guid", UUID.randomUUID().toString());
        request.addHeader("Request-Guid", UUID.randomUUID().toString());
        request.addHeader("Origin", "https://dom.gosuslugi.ru");
        request.addHeader("Sec-Fetch-Site", "same-origin");
        request.addHeader("Sec-Fetch-Mode", "cors");
        request.addHeader("Sec-Fetch-Dest", "empty");
        request.addHeader("Referer", "https://dom.gosuslugi.ru/");
        request.addHeader("Accept-Encoding", "gzip, deflate, br");
        request.addHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        request.addHeader("Priority", "u=1, i");
        request.addHeader("Connection", "close");
        request.setEntity(new StringEntity(body));
        return client.execute(request, response -> {
            if (response.getStatusLine().getStatusCode() == 200) {
                InputStream is = response.getEntity().getContent();
                return CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
            }
            return null;
        });
    }

    private static String GET(String url) throws IOException, InterruptedException {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Host,Content-Length,Connection");
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);
        request.addHeader("Host", "dom.gosuslugi.ru");
        request.addHeader("State-Guid", "/houses");
        request.addHeader("Sec-Ch-Ua", "\"Chromium\";v=\"123\", \"Not:A-Brand\";v=\"8\"");
        request.addHeader("Sec-Ch-Ua-Platform", "Windows");
        request.addHeader("Sec-Ch-Ua-Mobile", "?0");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.6312.122 Safari/537.36");
        request.addHeader("Content-Type", "application/json;charset=UTF-8");
        request.addHeader("Accept", "application/json; charset=utf-8");
        request.addHeader("Session-Guid", UUID.randomUUID().toString());
        request.addHeader("Request-Guid", UUID.randomUUID().toString());
        request.addHeader("Origin", "https://dom.gosuslugi.ru");
        request.addHeader("Sec-Fetch-Site", "same-origin");
        request.addHeader("Sec-Fetch-Mode", "cors");
        request.addHeader("Sec-Fetch-Dest", "empty");
        request.addHeader("Referer", "https://dom.gosuslugi.ru/");
        request.addHeader("Accept-Encoding", "gzip, deflate, br");
        request.addHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        request.addHeader("Priority", "u=1, i");
        request.addHeader("Connection", "close");
        return client.execute(request, response -> {
            if (response.getStatusLine().getStatusCode() == 200) {
                InputStream is = response.getEntity().getContent();
                return CharStreams.toString(new InputStreamReader(
                        is, Charsets.UTF_8));
            }
            return null;
        });
    }

    private static ArrayList<String> getCodes(String url) throws IOException, InterruptedException {
        String response = null;
        while (response == null) {
            response = GET(url);
            if (!Objects.equals(response, "[]")) {
                ArrayList<String> codes = new ArrayList<>();
                JsonArray responseJson = JsonParser.parseString(response).getAsJsonArray();
                responseJson.forEach(item ->
                        codes.add(item.getAsJsonObject().get("aoGuid").getAsString()));
                return codes;
            }
        }

        return new ArrayList<>();
    }

    private static String buildURL(String baseUrl, String regionCode, String cityCode, String areaCode, String settlementCode, String streetCode, String elementCode, int itemsPerPage) {
        baseUrl += "&itemsPerPage=" + itemsPerPage;
        if (regionCode != null) {
            baseUrl += "&regionCode=" + regionCode;
        }
        if (cityCode != null) {
            baseUrl += "&cityCode=" + cityCode;
        }
        if (settlementCode != null) {
            baseUrl += "&settlementCode=" + settlementCode;
        }
        if (areaCode != null) {
            baseUrl += "&areaCode=" + areaCode;
        }
        if (streetCode != null) {
            baseUrl += "&streetCode=" + streetCode;
        }
        if (elementCode != null) {
            baseUrl += "&planningStructureElementCode=" + elementCode;
        }
        return baseUrl;
    }

    private static String getHousesInPage(String regionCode, String cityCode, String streetCode, String areaCode, String settlementCode, String elementCode, int pageIndex, int elementsPerPage) throws IOException, InterruptedException {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Host,Content-Length,Connection");
        String json = "{\"fiasHouseCodeList\":null,\"estStatus\":null," +
                "\"strStatus\":null,\"calcCount\":true,\"houseConditionRefList\":null," +
                "\"houseTypeRefList\":[\"1\"],\"houseManagementTypeRefList\":null," +
                "\"cadastreNumber\":null,\"oktmo\":null,\"statuses\":[\"APPROVED\"],\"regionProperty\":null,\"municipalProperty\":null,\"hostelTypeCodes\":null";
        if (cityCode != null) {
            json += ", \"cityCode\": \"" + cityCode + "\"";
        }
        if (regionCode != null) {
            json += ", \"regionCode\": \"" + regionCode + "\"";
        }
        if (settlementCode != null) {
            json += ", \"settlementCode\": \"" + settlementCode + "\"";
        }
        if (areaCode != null) {
            json += ", \"areaCode\": \"" + areaCode + "\"";
        }
        if (streetCode != null) {
            json += ", \"streetCode\": \"" + streetCode + "\"";
        }
        if (elementCode != null) {
            json += ", \"planningStructureElementCode\": \"" + elementCode + "\"";
        }
        json += "}";
        String url = String.format("https://dom.gosuslugi.ru/homemanagement/api/rest/services/houses/public/searchByAddress?pageIndex=%d&elementsPerPage=%d", pageIndex, elementsPerPage);
        return POST(url, json);
    }

    private static JsonArray getHousesInStreet(String regionCode, String cityCode, String areaCode, String settlementCode, String streetCode, String elementCode, int elementsPerPage) throws IOException, InterruptedException {

        boolean flag = true;
        int pageIndex = 1;
        JsonArray houses = new JsonArray();
        while (flag) {
            String response = null;
            response = getHousesInPage(regionCode, cityCode, streetCode, areaCode, settlementCode, elementCode, pageIndex, elementsPerPage);
            if (response != null) {
                JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
                JsonArray items = responseJson.getAsJsonArray("items");
                if (items.size() > 0) {
                    responseJson.getAsJsonArray("items").forEach(houses::add);
                    pageIndex++;
                } else {
                    flag = false;
                }
            } else if (response == null) {
                System.out.println("blocked...");
                Thread.sleep(20000);
            }
        }
        return houses;
    }

    private static ArrayList<String> requestObjects(String url, int itemsPerPage, String regionCode, String areaCode, String cityCode, String settlementCode, String elementCode) throws IOException, InterruptedException {
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Host,Content-Length,Connection");
        boolean flag = true;
        int pageIndex = 1;
        ArrayList<String> codes = new ArrayList<>();
        url = buildURL(url, regionCode, cityCode, areaCode, settlementCode, null, elementCode, itemsPerPage);
        url += "&page=" + pageIndex;
        while (flag) {
            ArrayList<String> codesInPage = getCodes(url);
            if (codesInPage.size() > 0) {
                codes.addAll(codesInPage);
                url = url.substring(0, url.length() - 1);
                pageIndex++;
                url += pageIndex;
            } else {
                flag = false;
            }
        }
        return codes;
    }

    private static Document getInfoAboutHouse(JsonObject houseJson) throws InterruptedException, IOException {
        JsonObject houseAddressJson = houseJson.getAsJsonObject("address");
        Document house = new Document();
        HashMap<String, String> params = new HashMap<>();
        params.put("Тип фундамента ", "foundationType");
        params.put("Тип внутренних стен", "wallMaterial");
        params.put("Общая площадь жилых помещений", "areaResidential");
        params.put("Общая площадь нежилых помещений, за исключением помещений общего пользования", "areaNonResidential");
        params.put("Общая площадь помещений общего пользования в многоквартирном доме", "areaCommonProperty");
        params.put("Количество подъездов в многоквартирном доме", "entranceCount");
        params.put("Количество лифтов ", "elevatorsCount");
        params.put("Количество жилых помещений (квартир)", "livingFlatsCount");
        params.put("Количество нежилых помещений", "unlivingFlatsCount");
        params.put("Количество этажей", "floorsCount");
        params.put("Тип внутридомовой системы отопления", "heatingType");
        params.put("Тип внутридомовой инженерной системы холодного водоснабжения", "coldWaterType");
        params.put("Тип внутридомовой инженерной системы горячего водоснабжения", "hotWaterType");
        params.put("Тип внутридомовой инженерной системы водоотведения", "sewerageType");
        params.put("Количество вводов внутридомовой инженерной системы электроснабжения в многоквартирный дом (количество точек поставки)", "electricalEntriesCount");
        params.put("Тип внутридомовой инженерной системы газоснабжения", "gasType");


        house.put("Region", getField(houseAddressJson.getAsJsonObject("region").get("formalName")).get("value"));
        house.put("RegionID", getField(houseAddressJson.getAsJsonObject("region").get("aoGuid")).get("value"));
        house.put("RegionCode", getField(houseAddressJson.getAsJsonObject("region").get("regionCode")).get("value"));
        house.put("PostalCode", getField(houseAddressJson.getAsJsonObject("region").get("postalCode")).get("value"));
        if (houseAddressJson.get("area").isJsonNull()) {
            house.put("Municipalitet", getField(houseAddressJson.getAsJsonObject("city").get("shortName")).get("value") + " " +
                    getField(houseAddressJson.getAsJsonObject("city").get("formalName")).get("value"));
            house.put("BlockID", getField(houseAddressJson.getAsJsonObject("city").get("formalName")).get("value"));

        } else {
            house.put("BlockID", getField(houseAddressJson.getAsJsonObject("area").get("formalName")).get("value"));
            house.put("MunicipalitetID", getField(houseAddressJson.getAsJsonObject("settlement").get("aoGuid")).get("value"));
            house.put("Municipalitet",
                    getField(houseAddressJson.getAsJsonObject("area").get("shortName")) + " " +
                            getField(houseAddressJson.getAsJsonObject("area").get("formalName")).get("value") + " " +
                            getField(houseAddressJson.getAsJsonObject("settlement").get("shortName")).get("value") + " " +
                            getField(houseAddressJson.getAsJsonObject("settlement").get("formalName")).get("value"));
        }
        if (!houseAddressJson.get("street").isJsonNull()) {
            house.put("Street",
                    getField(houseAddressJson.getAsJsonObject("street").get("shortName")).get("value") + " " +
                            getField(houseAddressJson.getAsJsonObject("street").get("formalName")).get("value"));
            house.put("HouseNumber", getField(houseAddressJson.getAsJsonObject("house").get("houseNumber")).get("value"));
            house.put("AddressDesc", getField(houseAddressJson.get("formattedAddress")).get("value"));
        } else {
            house.put("Street", null);
        }

        house.put("houseType", getField(houseJson.getAsJsonObject("houseType").get("houseTypeName")).get("value"));
        house.put("projectType", getField(houseJson.get("planSeries")).get("value"));
        house.put("exploitationStartYear", getField(houseJson.get("operationYear")).get("value"));
        house.put("builtYear", getField(houseJson.get("buildingYear")).get("value"));
        house.putAll(getFieldFromPassport(houseJson.get("guid").getAsString(), null, params));
//        house.putAll(getFieldFromPassport(houseJson.get("guid").getAsString(), "3.1", params));
//        house.putAll(getFieldFromPassport(houseJson.get("guid").getAsString(), "3.2", params));
//        house.putAll(getFieldFromPassport(houseJson.get("guid").getAsString(), "4", params));
//        house.putAll(getFieldFromPassport(houseJson.get("guid").getAsString(), "5", params));
//        house.putAll(getFieldFromPassport(houseJson.get("guid").getAsString(), "6", params));
//        house.putAll(getFieldFromPassport(houseJson.get("guid").getAsString(), "7", params));
//        house.putAll(getFieldFromPassport(houseJson.get("guid").getAsString(), "8", params));
//        house.putAll(getFieldFromPassport(houseJson.get("guid").getAsString(), "9", params));

        return Document.parse(new Gson().toJson(house));
    }

    private static Document getField(JsonElement field) {
        try {
            return new Document().append("value", field.getAsInt());
        } catch (Exception e) {
        }
        try {
            return new Document().append("value", field.getAsDouble());
        } catch (Exception e) {
        }
        try {
            return new Document().append("value", field.getAsString());
        } catch (Exception e) {
        }
        return new Document().append("value", null);
    }

    private static Integer getCountOfHouses(String regionCode, String cityCode, String areaCode, String settlementCode) throws IOException, InterruptedException {
        boolean flag = true;
        while (flag) {
            String response = null;
            response = getHousesInPage(regionCode, cityCode, null, areaCode, settlementCode, null, 1, 10);
            if (response != null) {
                JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();
                return responseJson.get("total").isJsonNull() ? 0 : responseJson.get("total").getAsInt();

            }
            System.out.println("blocked...");
            Thread.sleep(20000);

        }
        return -1;
    }

    private static Document getFieldFromPassport(String houseGuid, String passportParameterCode, HashMap<String, String> parameterNames) throws InterruptedException, IOException {

        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Host,Content-Length,Connection");
        String url = "https://dom.gosuslugi.ru/homemanagement/api/rest/services/passports/search";
        String json;
        if (passportParameterCode == null) {
            json = "{\"houseGuid\":\"" + houseGuid + "\",\"passportParameterCode\":" + null + ",\"page\":1,\"itemsPerPage\":500}";

        } else {
            json = "{\"houseGuid\":\"" + houseGuid + "\",\"passportParameterCode\":\"" + passportParameterCode + "\",\"page\":1,\"itemsPerPage\":500}";

        }
        String response = POST(url, json);
        while (response == null) {
            Thread.sleep(20000);
            response = POST(url, json);
        }


        JsonArray parameters = JsonParser.parseString(response).getAsJsonObject().getAsJsonArray("parameters");
        Document values = new Document();
        Document result = new Document();
        parameters.forEach(item -> values.put(
                getField(item.getAsJsonObject().get("name")).get("value").toString(),
                getField(item.getAsJsonObject().get("value")).get("value")));
        for (String parameterName : parameterNames.keySet()) {
            if (values.containsKey(parameterName)) {
                result.put(parameterNames.get(parameterName), values.get(parameterName));
            }
        }
        return result;
    }

    @Override
    protected int mainHarvest() throws IOException, InterruptedException {
        String regionCode = getParams().getString("regionGuid");
        MongoClient client = MongoClients.create(getParams().getString("ConnectionString"));
        MongoCollection<Document> housesCollection = client.getDatabase(getParams().getString("database")).getCollection(
                getParams().getString("dataset") + "_inProgress");
        housesCollection.drop();
        int counter = 1;
        //Получаем список кодов городов в регионе
        ArrayList<String> cityCodes = requestObjects(cityURL, 100, regionCode, null, null, null, null);
        for (String cityCode:cityCodes) {
            Integer countOfHouses = getCountOfHouses(regionCode, cityCode, null, null);
            if (countOfHouses > 0 && countOfHouses <= 2000) {
                ArrayList<String> ids = new ArrayList<>();
                JsonArray housesInStreet = getHousesInStreet(regionCode, cityCode, null, null, null, null, 100);
                for (int m = 0; m < housesInStreet.size(); m++) {
                    String id = getField(housesInStreet.get(m).getAsJsonObject().getAsJsonObject("address").getAsJsonObject("house").get("houseGuid")).get("value").toString();
                    if (!ids.contains(id)) {
                        ids.add(id);
                        housesCollection.insertOne(getInfoAboutHouse(housesInStreet.get(m).getAsJsonObject()));
                    }
                }
            } else {
                //Получаем список кодов элементов планировочной структуры
                ArrayList<String> elementCodes = requestObjects(elementURL, 100, regionCode, null, cityCode, null, null);
                for (String elementCode : elementCodes) {
                    ArrayList<String> ids = new ArrayList<>();
                    JsonArray housesInStreet = getHousesInStreet(regionCode, cityCode, null, null, null, elementCode, 100);
                    for (int m = 0; m < housesInStreet.size(); m++) {
                        String id = getField(housesInStreet.get(m).getAsJsonObject().getAsJsonObject("address").getAsJsonObject("house").get("houseGuid")).get("value").toString();
                        if (!ids.contains(id)) {
                            ids.add(id);
                            housesCollection.insertOne(getInfoAboutHouse(housesInStreet.get(m).getAsJsonObject()));
                        }
                    }
                }
                //Получаем список кодов населенных пунктов, относящихся к  городу
                ArrayList<String> settlementCodes = requestObjects(settlementURL, 100, regionCode, null, cityCode, null, null);
                for (String settlementCode : settlementCodes) {
                    countOfHouses = getCountOfHouses(regionCode, cityCode, null, settlementCode);
                    if (countOfHouses > 0 && countOfHouses <= 2000) {
                        ArrayList<String> ids = new ArrayList<>();
                        JsonArray housesInStreet = getHousesInStreet(regionCode, cityCode, null, null, null, null, 100);
                        for (int m = 0; m < housesInStreet.size(); m++) {
                            String id = getField(housesInStreet.get(m).getAsJsonObject().getAsJsonObject("address").getAsJsonObject("house").get("houseGuid")).get("value").toString();
                            if (!ids.contains(id)) {
                                ids.add(id);
                                housesCollection.insertOne(getInfoAboutHouse(housesInStreet.get(m).getAsJsonObject()));
                            }
                        }
                    } else {
                        ArrayList<String> ids = new ArrayList<>();
                        //получаем список кодов улиц населенного пункта, относящегося к городу
                        ArrayList<String> streetCodes = requestObjects(streetURL, 100, regionCode, null, cityCode, settlementCode, null);
                        for (String streetCode : streetCodes) {
                            JsonArray housesInStreet = getHousesInStreet(regionCode, cityCode, null, settlementCode, streetCode, null, 100);
                            for (int m = 0; m < housesInStreet.size(); m++) {
                                String id = getField(housesInStreet.get(m).getAsJsonObject().getAsJsonObject("address").getAsJsonObject("house").get("houseGuid")).get("value").toString();
                                if (!ids.contains(id)) {
                                    ids.add(id);
                                    housesCollection.insertOne(getInfoAboutHouse(housesInStreet.get(m).getAsJsonObject()));
                                }
                            }
                        }
                    }
                }


                ArrayList<String> ids = new ArrayList<>();
                //получаем список кодов улиц города
                ArrayList<String> streetCodes = requestObjects(streetURL, 100, regionCode, null, cityCode, null, null);
                for (String streetCode : streetCodes) {
                    JsonArray housesInStreet = getHousesInStreet(regionCode, cityCode, null, null, streetCode, null, 100);
                    for (int m = 0; m < housesInStreet.size(); m++) {
                        String id = getField(housesInStreet.get(m).getAsJsonObject().getAsJsonObject("address").getAsJsonObject("house").get("houseGuid")).get("value").toString();
                        if (!ids.contains(id)) {
                            ids.add(id);
                            housesCollection.insertOne(getInfoAboutHouse(housesInStreet.get(m).getAsJsonObject()));
                        }
                    }
                }

            }
        }

        //получаем список кодов районов региона
        ArrayList<String> areaCodes = requestObjects(areaURL, 100, regionCode, null, null, null, null); //нет регионов с количество  городов большим ста
        for (String areaCode : areaCodes) {
            Integer countOfHouses = getCountOfHouses(regionCode, null, areaCode, null);
            if (countOfHouses > 0 && countOfHouses <= 2000) {
                ArrayList<String> ids = new ArrayList<>();
                JsonArray housesInStreet = getHousesInStreet(regionCode, null, areaCode, null, null, null, 100);
                for (int m = 0; m < housesInStreet.size(); m++) {
                    String id = getField(housesInStreet.get(m).getAsJsonObject().getAsJsonObject("address").getAsJsonObject("house").get("houseGuid")).get("value").toString();
                    if (!ids.contains(id)) {
                        ids.add(id);
                        housesCollection.insertOne(getInfoAboutHouse(housesInStreet.get(m).getAsJsonObject()));
                    }
                }
            } else {
                //получаем список кодов населенных пунктов, относящихся к району региона
                ArrayList<String> settlementCodes = requestObjects(settlementURL, 100, regionCode, areaCode, null, null, null);
                for (String settlementCode : settlementCodes) {
                    countOfHouses = getCountOfHouses(regionCode, null, areaCode, settlementCode);
                    if (countOfHouses > 0 && countOfHouses <= 2000) {
                        ArrayList<String> ids = new ArrayList<>();
                        JsonArray housesInStreet = getHousesInStreet(regionCode, null, areaCode, settlementCode, null, null, 100);
                        for (int m = 0; m < housesInStreet.size(); m++) {
                            String id = getField(housesInStreet.get(m).getAsJsonObject().getAsJsonObject("address").getAsJsonObject("house").get("houseGuid")).get("value").toString();
                            if (!ids.contains(id)) {
                                ids.add(id);
                                housesCollection.insertOne(getInfoAboutHouse(housesInStreet.get(m).getAsJsonObject()));
                            }
                        }
                    } else {
                        ArrayList<String> ids = new ArrayList<>();
                        //получаем список кодов улиц населенного пункта, относящегося к району региона
                        ArrayList<String> streetCodes = requestObjects(streetURL, 100, regionCode, areaCode, null, settlementCode, null);
                        for (String streetCode : streetCodes) {
                            JsonArray housesInStreet = getHousesInStreet(regionCode, null, areaCode, settlementCode, streetCode, null, 100);
                            for (int m = 0; m < housesInStreet.size(); m++) {
                                String id = getField(housesInStreet.get(m).getAsJsonObject().getAsJsonObject("address").getAsJsonObject("house").get("houseGuid")).get("value").toString();
                                if (!ids.contains(id)) {
                                    ids.add(id);
                                    housesCollection.insertOne(getInfoAboutHouse(housesInStreet.get(m).getAsJsonObject()));
                                }
                            }
                        }
                    }
                }
            }
        }
        client.close();
        return (int) housesCollection.countDocuments();
    }
}