
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import mpi.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class MPI_Java {

    public static void main(String args[]) throws InterruptedException {
        MPI.Init(args);
        int me = MPI.COMM_WORLD.Rank();
        int tasks = MPI.COMM_WORLD.Size();
        final int[] pages = {12, 15, 10, 14, 11};
//        final int[] pages = {10, 14, 11};
        Document agent = null;
        try {
            agent = Jsoup.parse(new FileInputStream("/home/marcos/Desktop/MPI_Java/allagents.xml"), "UTF-8", "", org.jsoup.parser.Parser.xmlParser());
        } catch (IOException ex) {
            Logger.getLogger(MPI_Java.class.getName()).log(Level.SEVERE, null, ex);
        }
        Elements agents = agent.select("user-agent > String");
        TreeMap<Double, String> col = new TreeMap<>();
        int lastValue, actualValue;
        Document doc2;
        String gameLink;
        JSONObject response;
        Double pontos_real;
        double preco;
        final String tremorLink = "http://www.tremorgames.com/?action=tradeinprices&itemtype=";
        Document doc;
        int l_a, l_b;
        for (int i = 0; i < pages.length; i++) {
            try {
                //Para cada uma das páginas
                doc = Jsoup.connect(tremorLink + pages[i]).get();
            } catch (IOException ex) {
                i--;
                continue;
            }
            System.out.printf("\n<%d> Processando %s\n", me, doc.select("div.box_round").first().text());
            Elements itemList = doc.select("tr");
            l_a = 0 == me ? 1 : (itemList.size() / tasks) * me;
            l_b = me == (tasks - 1) ? itemList.size() : l_a + (itemList.size() / tasks);
            if (me == 0) {
                l_b--;
            }
//            System.out.printf("l_a = %d l_b = %d itens = %d\n", l_a, l_b, itemList.size());
            for (int j = l_a; j < l_b; j++) {//O primeiro é o cabeçalho da tabela e n tem href                    
                if (j % 50 == 0) {
                    System.out.printf("\n<%d> Processed: %d\t ItemName: %s\n", me, (j - l_a), itemList.get(j).select("td").first().text());
                }
                try {
                    //Página do Item do Tremor Games                
                    doc2 = Jsoup.connect(itemList.get(j).select("a").attr("href"))
                            .header("Accept-Language", "pt-BR,en-US;q=0.8,en;q=0.8")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                            .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118 Safari/537.36")
                            .header("Accept-Encoding", "gzip, deflate, sdch")
                            .header("Cookie", "PHPSESSID=403c2ddfcd984f0af2a8514efaa25262; tguserid=1259204; evercookie_png=1259204; evercookie_etag=1259204; evercookie_cache=1259204; uid=1259204; eccheck=0; __utmt=1; __utma=269348916.138314648.1428357214.1428763344.1428764750.10; __utmb=269348916.19.10.1428764750; __utmc=269348916; __utmz=269348916.1428764750.10.2.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided)")
                            .get();
                    //Página do Item do Market da Steam                         
                    gameLink = doc2.select("#productlink").first().attr("href");
                    if (gameLink.contains("http://steamcommunity.com/market/listings")) { //Poupa uma conexão errada                                                                                  
                        doc2 = Jsoup.connect(doc2.select("#productlink").first().attr("href"))
                                .header("Accept-Language", "pt-BR,en-US;q=0.8,en;q=0.8")
                                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                                .header("Accept-Encoding", "gzip, deflate, sdch")
                                .header("Cookie", "recentlyVisitedAppHubs=440; strInventoryLastContext=753_0; steamLogin=76561198049006380%7C%7C99A07FAD33D0523C1A0047C795423450B1E5DC98; sessionid=e5d2bbbdd9b13d9287454fb1; webTradeEligibility=%7B%22allowed%22%3A1%2C%22allowed_at_time%22%3A0%2C%22steamguard_required_days%22%3A15%2C%22sales_this_year%22%3A43%2C%22max_sales_per_year%22%3A200%2C%22forms_requested%22%3A0%2C%22new_device_cooldown_days%22%3A7%7D; timezoneOffset=-10800,0; __utma=268881843.1547344608.1428332143.1428378196.1428410634.6; __utmb=268881843.0.10.1428410634; __utmc=268881843; __utmz=268881843.1428410634.6.5.utmcsr=store.steampowered.com|utmccn=(referral)|utmcmd=referral|utmcct=/")
                                .userAgent(agents.get((int) (Math.random() * agents.size())).text())
                                .get();
                        if (doc2.select("div.market_listing_table_message").first().text().contains("Não há anúncios para este item.")) {
                            //Caso onde não há itens a venda
                            continue;
                        }
                        String gameCode = doc2.select("script").last().toString();
                        int pos = gameCode.indexOf("pread(") + 7;
                        if (pos > 6) { // itens agrupados
                            gameCode = gameCode.substring(pos, gameCode.indexOf(";", pos) - 2);
                            response = new JSONObject(new Scanner(new URL("http://steamcommunity.com/market/itemordershistogram?country=BR&language=brazilian&currency=7&item_nameid=" + gameCode)
                                    .openStream())
                                    .nextLine());
                            try {
                                doc2 = Jsoup.parseBodyFragment(response.getString("sell_order_table"));
                            } catch (JSONException e) {
                                continue;
                            }
                            Elements es = doc2.select("td");
                            for (int k = 0; k < es.size(); k += 2) {
                                preco = NumberFormat.getInstance(Locale.FRANCE).parse(es.get(k).text().substring(3, es.get(k).text().length())).doubleValue();
                                pontos_real = Double.parseDouble(itemList.get(j).select("td").get(1).text()) / preco;
//                                System.out.printf("\nPontos\\Real = %.0f\\%.2f = %.4f \tItemName = { %s }\n", Double.parseDouble(itemList.get(j).select("td").get(1).text()), preco, pontos_real, itemList.get(j).select("td").first().text());
                                if (pontos_real >= 400) {
                                    System.out.printf("\nPontos\\Real = %.0f\\%.2f = %.4f \tItemName = { %s }\n", Double.parseDouble(itemList.get(j).select("td").get(1).text()), preco, pontos_real, itemList.get(j).select("td").first().text());
                                    if (col.containsKey(pontos_real)) {
                                        col.put(pontos_real, col.get(pontos_real) + " , " + itemList.get(j).select("td").first().text());
                                    } else {
                                        col.put(pontos_real, itemList.get(j).select("td").first().text());
                                    }
                                }
                            }
                        } else { //É item vendido separadamente
                            response = (new JSONObject(new Scanner(new URL(gameLink + "/render?start=0&count=10&currency=7&language=english&format=json")
                                    .openStream())
                                    .nextLine()))
                                    .getJSONObject("listinginfo");
                            lastValue = 0;
                            for (String key : response.keySet()) {
                                actualValue = response.getJSONObject(key).getInt("price");
                                if (lastValue != actualValue) {
                                    lastValue = actualValue;
                                    preco = actualValue;
                                    pontos_real = 100 * Double.parseDouble(itemList.get(j).select("td").get(1).text()) / actualValue;
//                                    System.out.printf("\nPontos\\Real = %.0f\\%.2f = %.4f \tItemName = { %s }\n", Double.parseDouble(itemList.get(j).select("td").get(1).text()), preco, pontos_real, itemList.get(j).select("td").first().text());
                                    if (pontos_real >= 400) {
                                        System.out.printf("\nPontos\\Real = %.0f\\%.2f = %.4f \tItemName = { %s }\n", Double.parseDouble(itemList.get(j).select("td").get(1).text()), preco, pontos_real, itemList.get(j).select("td").first().text());
                                        if (col.containsKey(pontos_real)) {
                                            col.put(pontos_real, col.get(pontos_real) + " , " + itemList.get(j).select("td").first().text());
                                        } else {
                                            col.put(pontos_real, itemList.get(j).select("td").first().text());
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (HttpStatusException hu) {
                    if (hu.getStatusCode() == 429) {
//                        System.out.printf("S...");
                        Thread.sleep(100);
                        j--;
                    }
                } catch (SocketTimeoutException so) {
//                    System.out.printf("so...");
                    j--;
                    System.gc();
                } catch (JSONException h) {
                    System.out.println("Item " + itemList.get(j).select("td").first().text() + " deu JSONException");
                    j--;
                    System.gc();
                } catch (NumberFormatException n) {
//                    System.out.printf("n...");
                    System.gc();
                } catch (IOException io) {
//                    System.out.printf("io...");
                    Thread.sleep(100);
                    System.gc();
                    j--;
                } catch (MPIException iolp) {
//                    System.out.println("Item " + itemList.get(j).select("td").first().text() + " deu MPIException");
                    System.gc();
                } catch (NullPointerException nul) {
//                    System.out.println("Não achou o item no site");
                    System.gc();
                } catch (Exception el) {
//                    System.out.println("Item " + itemList.get(j).select("td").first().text() + " deu Exception");
                    System.gc();
                }
            }
            System.out.println("Mapa Temporário");
            for (Map.Entry<Double, String> entrySet : col.entrySet()) {
                Double key = entrySet.getKey();
                String value = entrySet.getValue();
                System.out.println("Preco\\Real = " + key + "\tItem =" + value);
            }
        }
        if (me == 0) {
            //Recebe os mapas de cada um            
            Object[] finalResp = new Object[1];
            for (int j = 1; j < tasks; j++) {
                System.out.println("Recebendo mapas...");
                MPI.COMM_WORLD.Recv(finalResp, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, 999);
                col.putAll(((TreeMap<Double, String>) finalResp[0]));
            }
            System.out.println("Todos os mapas recebidos!");
            //Imprime o mapa após receber
            System.out.println("\n\n");
            for (Map.Entry<Double, String> entrySet : col.entrySet()) {
                Double key = entrySet.getKey();
                String value = entrySet.getValue();
                System.out.println("Preco\\Real = " + key + "\tItem =" + value);
            }
        } else {
            //envia o mapa para 0
            Object[] myResp = new Object[1];
            myResp[0] = col;
            MPI.COMM_WORLD.Send(myResp, 0, 1, MPI.OBJECT, 0, 999);
        }
        System.out.println("Final do programa");
        MPI.Finalize();
    }
}
