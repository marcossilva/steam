
package javaapplication2;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BestCostBenefit_1 {

    public static void main(String[] args) throws IOException {
        int[] pages = {12, 15, 10, 14, 11};
        String tremorLink = "http://www.tremorgames.com/?action=tradeinprices&itemtype=", valueItem;
        Document doc, doc2;
        double bestOffer = 0, lastValue = 0, actualValue, total_time = 0;
        Map<Double, String> col = new TreeMap<>();
        Double pontos_real;
        for (int i = 0; i < pages.length; i++) {//Para cada uma das páginas
            doc = Jsoup.connect(tremorLink + pages[i]).get();
            Elements itemList = doc.select("tr");
            for (int j = 1; j < itemList.size(); j++) {//O primeiro é o cabeçalho da tabela e n tem href
                long startTime = System.nanoTime();
                try {
                    //Página do Item do Tremor Games                
                    doc2 = Jsoup.connect(itemList.get(j).select("a").attr("href")).header("Cookie", "PHPSESSID=2d69c92e61b2e70b84056cc92e14d1ca; tguserid=1259204; evercookie_png=1259204; evercookie_etag=1259204; evercookie_cache=1259204; uid=1259204; eccheck=0; __utmt=1; __utma=269348916.974876551.1427771853.1428087788.1428097277.20; __utmb=269348916.10.10.1428097277; __utmc=269348916; __utmz=269348916.1427777064.3.2.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided)").get();
                    //Página do Item do Market da Steam                                            
                    doc2 = Jsoup.connect(doc2.select("#productlink").first().attr("href")).header("Cookie", "strInventoryLastContext=440_2; sessionid=a7d8731062ced06586588bb8; wishlist_sort2=added; recentlyVisitedAppHubs=244930%2C207140; steamCountry=BR%7C5cc135e92bf5a054b355924f9a64cdbd; steamLogin=76561198049006380%7C%7C99A07FAD33D0523C1A0047C795423450B1E5DC98; webTradeEligibility=%7B%22allowed%22%3A0%2C%22reason%22%3A2048%2C%22allowed_at_time%22%3A1428365417%2C%22steamguard_required_days%22%3A15%2C%22sales_this_year%22%3A11%2C%22max_sales_per_year%22%3A200%2C%22forms_requested%22%3A0%2C%22new_device_cooldown_days%22%3A7%7D; timezoneOffset=-10800,0; __utma=268881843.1628666419.1427760585.1428116364.1428122319.24; __utmb=268881843.0.10.1428122319; __utmc=268881843; __utmz=268881843.1428122319.24.17.utmcsr=tremorgames.com|utmccn=(referral)|utmcmd=referral|utmcct=/index.php").get();
                    if (doc2.baseUri().contains("http://steamcommunity.com/market/listings")) {
                        String gameCode = doc2.select("script").last().toString();
                        int pos = gameCode.indexOf("pread(") + 7;
                        if (pos > 6) { // itens agrupados
                            gameCode = gameCode.substring(pos, gameCode.indexOf(";", pos) - 2);
                            JSONObject response = new JSONObject(new Scanner(new URL("http://steamcommunity.com/market/itemordershistogram?country=BR&language=brazilian&currency=1&item_nameid=" + gameCode).openStream()).nextLine());
                            doc2 = Jsoup.parseBodyFragment(response.getString("sell_order_table"));
                            Elements es = doc2.select("td");
                            for (int k = 0; k < es.size(); k += 2) {
                                pontos_real = Double.parseDouble(itemList.get(j).select("td").get(1).text()) / Double.parseDouble(es.get(k).text().substring(1, es.get(k).text().indexOf(".") + 3));
//                                System.out.println("Preco: \t" + es.get(k).text().substring(1, es.get(k).text().indexOf(".") + 3));
//                                System.out.println("INSERTING...\nPontos\\Real = " + pontos_real + "\t\t\tItemName = {" + itemList.get(j).select("td").get(0).text() + "}");
                                if (col.containsKey(pontos_real)) {
                                    col.put(pontos_real, col.get(pontos_real) + " , " + itemList.get(j).select("td").get(0).text());
                                } else {
                                    col.put(pontos_real, itemList.get(j).select("td").get(0).text());
                                }
                                if (pontos_real > bestOffer) {
                                    bestOffer = pontos_real;
                                    System.out.println("MELHOR CUSTO BENEFÍCIO TEMPORÁRIO\nPontos\\Real = " + pontos_real + "\t\t\tItemName = {" + itemList.get(j).select("td").get(0).text() + "}");
                                }
                            }
                        } else { //É item vendido separadamente
                            Elements es = doc2.select("#searchResultsRows").select("span.market_listing_price:nth-child(2n-1)");
                            lastValue = 0;
                            for (Element e : es) {
                                valueItem = e.text().substring(e.text().indexOf("$") + 2, e.text().indexOf(",") + 3);
                                if (!valueItem.contains("o")) {
                                    actualValue = NumberFormat.getNumberInstance(Locale.FRANCE).parse(valueItem).doubleValue();
                                    if (lastValue != actualValue) {
                                        lastValue = actualValue;
                                        pontos_real = Double.parseDouble(itemList.get(j).select("td").get(1).text()) / actualValue;
                                        if (col.containsKey(pontos_real)) {
                                            col.put(pontos_real, col.get(pontos_real) + " , " + itemList.get(j).select("td").get(0).text());
                                        } else {
                                            col.put(pontos_real, itemList.get(j).select("td").get(0).text());
                                        }
                                        if (pontos_real > bestOffer) {
                                            bestOffer = pontos_real;
                                            System.out.println("MELHOR CUSTO BENEFÍCIO TEMPORÁRIO\nPontos\\Real = " + pontos_real + "\t\t\tItemName = {" + itemList.get(j).select("td").get(0).text() + "}");
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (SocketTimeoutException to) {
                    System.out.println("Está demorando esse socket aí hem...");
                    System.out.println("j = " + j);
                    j--;
                    continue;
                } catch (ParseException ex) {
                    System.out.println("Não soube parsear o item " + itemList.get(j).select("td").get(0).text());
                    System.out.println("j = " + j);
                    j--;
                    continue;
                }
                double estimatedTime = System.nanoTime() - startTime;
                total_time += (estimatedTime / 1000000000);
                System.out.println(total_time);
                System.out.println("\rItens no Mapa: " + col.size() + "\tÚltimo Item Analisado: " + itemList.get(j).select("td").get(0).text()
                        + "\tTempoItem: " + estimatedTime / 1000000000 + "s\tETA: " + (total_time / j) * (itemList.size() - j) + "s");
            }
            System.out.println("Acabei site " + (i + 1) + "°");
        }
        //Imprime mapa
        for (Map.Entry<Double, String> entrySet : col.entrySet()) {
            Double key = entrySet.getKey();
            String value = entrySet.getValue();
            System.out.println("Pontos\\Real = " + key + "\t\tItemNames = {" + value + "}");
        }
    }
}