
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import mpi.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MPI_Java {

    public static void main(String args[]) throws IOException {
        MPI.Init(args);
        int me = MPI.COMM_WORLD.Rank();
        int tasks = MPI.COMM_WORLD.Size();
        final int[] pages = {12, 15, 10, 14, 11};
        TreeMap<Double, String> col = new TreeMap<>();
        if (me == 0) {
            final String tremorLink = "http://www.tremorgames.com/?action=tradeinprices&itemtype=";
            Document doc;
            Object[] toGo = new Object[3];
            int[] size = new int[1];
            for (int i = 0; i < pages.length; i++) {//Para cada uma das páginas
                doc = Jsoup.connect(tremorLink + pages[0]).get();
                Elements itemList = doc.select("tr");
                size[0] = itemList.size() / tasks;
                for (int j = 1; j < tasks; j++) { //Avisa pra cada task quantos tarefas ela será encabida
                    MPI.COMM_WORLD.Send(size, 0, 1, MPI.INT, j, 199);//tag 199 = Prepare
                }
                for (int j = 1; j < itemList.size(); j++) {//O primeiro é o cabeçalho da tabela e n tem href                    
                    for (int k = 1; k < tasks; k++) {
                        toGo[0] = itemList.get(j).select("a").attr("href"); //Link para market da Steam
                        toGo[1] = itemList.get(j).select("td").get(0).text(); //Nome do jogo
                        toGo[2] = Double.parseDouble(itemList.get(j).select("td").get(1).text()); //Pontos do Jogo
                        j++;
                        MPI.COMM_WORLD.Send(toGo, 0, 3, MPI.OBJECT, k, 200); //tag 200 = loop piece
                    }
                    if ((j + tasks) >= itemList.size()) { //Garante a divisão "perfeita" mas perde os últimos itens da lista
                        break;
                    }
                }
            }
            //Recebe os mapas de cada um
            Object[] finalResp = new Object[1];
            for (int j = 1; j < tasks; j++) {
                MPI.COMM_WORLD.Recv(finalResp, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, 999);
                col.putAll(((TreeMap<Double, String>) finalResp[0]));
            }
            System.out.println("\n\n");
            for (Map.Entry<Double, String> entrySet : col.entrySet()) {
                Double key = entrySet.getKey();
                String value = entrySet.getValue();
                System.out.println("Preco\\Real = " + key + "\tItem =" + value);
            }
        } else {
            int lastValue, actualValue;
            Document doc2;
            String gameLink;
            JSONObject response;
            Double pontos_real;
            int[] local_loop = new int[1];
            Object[] element = new Object[3];
            for (int j = 0; j < pages.length; j++) {//Para cada uma das páginas
                MPI.COMM_WORLD.Recv(local_loop, 0, 1, MPI.INT, 0, 199);//tag 199 = Prepare
                for (int i = 0; i < local_loop[0]; i++) {
//                    System.out.println(me + " em loop " + i);
                    MPI.COMM_WORLD.Recv(element, 0, 3, MPI.OBJECT, 0, 200);
                    try {
                        //Página do Item do Tremor Games                
                        doc2 = Jsoup.connect(element[0].toString())
                                .header("Accept-Language", "pt-BR,en-US;q=0.8,en;q=0.8")
                                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118 Safari/537.36")
                                .header("Accept-Encoding", "gzip, deflate, sdch")
                                .header("Cookie", "PHPSESSID=72280da7e5e96308aadc5bd1f23cb737; tguserid=1259204; eccheck=0; evercookie_png=1259204; evercookie_etag=1259204; evercookie_cache=1259204; uid=1259204; __utmt=1; __utma=269348916.138314648.1428357214.1428357214.1428376670.2; __utmb=269348916.3.10.1428376670; __utmc=269348916; __utmz=269348916.1428357214.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)")
                                .get();
                        //Página do Item do Market da Steam                         
                        gameLink = doc2.select("#productlink").first().attr("href");
                        if (gameLink.contains("http://steamcommunity.com/market/listings")) { //Poupa uma conexão errada    
                            doc2 = Jsoup.connect(doc2.select("#productlink").first().attr("href"))
                                    .header("Cookie", "PHPSESSID=72280da7e5e96308aadc5bd1f23cb737; tguserid=1259204; eccheck=0; evercookie_png=1259204; evercookie_etag=1259204; evercookie_cache=1259204; uid=1259204; __utma=269348916.138314648.1428357214.1428357214.1428357214.1; __utmc=269348916; __utmz=269348916.1428357214.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)")
                                    .get();
                            String gameCode = doc2.select("script").last().toString();
                            int pos = gameCode.indexOf("pread(") + 7;
                            if (pos > 6) { // itens agrupados
                                gameCode = gameCode.substring(pos, gameCode.indexOf(";", pos) - 2);
                                response = new JSONObject(new Scanner(new URL("http://steamcommunity.com/market/itemordershistogram?country=BR&language=brazilian&currency=1&item_nameid=" + gameCode).openStream()).nextLine());
                                doc2 = Jsoup.parseBodyFragment(response.getString("sell_order_table"));
                                Elements es = doc2.select("td");
                                for (int k = 0; k < es.size(); k += 2) {
                                    pontos_real = ((Double) element[2]) / Double.parseDouble(es.get(k).text().substring(1, es.get(k).text().indexOf(".") + 3));
                                    System.out.printf("Pontos\\Real = %.4f \tItemName = { %s }\n", pontos_real, element[1].toString());
                                    if (pontos_real > 1200) {
//                                        System.out.printf("Pontos\\Real = %.4f \tItemName = { %s }\n", pontos_real, element[1].toString());
                                        if (col.containsKey(pontos_real)) {
                                            col.put(pontos_real, col.get(pontos_real) + " , " + element[1].toString());
                                        } else {
                                            col.put(pontos_real, element[1].toString());
                                        }
                                    }
                                }
                            } else { //É item vendido separadamente
                                response = (new JSONObject(new Scanner(new URL(gameLink + "/render?start=0&count=10&currency=7&language=english&format=json").openStream()).nextLine()))
                                        .getJSONObject("listinginfo");
                                lastValue = 0;
                                for (String key : response.keySet()) {
                                    actualValue = response.getJSONObject(key).getInt("price");
                                    if (lastValue != actualValue) {
                                        lastValue = actualValue;
                                        pontos_real = 100 * ((Double) element[2]) / actualValue;
                                        System.out.printf("Pontos\\Real = %.4f \tItemName = { %s }\n", pontos_real, element[1].toString());
                                        if (pontos_real > 1200) {
//                                            System.out.printf("Pontos\\Real = %.4f \tItemName = { %s }\n", pontos_real, element[1].toString());
                                            if (col.containsKey(pontos_real)) {
                                                col.put(pontos_real, col.get(pontos_real) + " , " + element[1].toString());
                                            } else {
                                                col.put(pontos_real, element[1].toString());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (SocketTimeoutException to) {
                        System.out.println("Está demorando esse socket aí hem...");
                        System.out.println("i = " + i);
                        i--;
                    } catch (JSONException h) {
                        System.out.println("Item " + element[1].toString() + " deu JSONException");
                        System.out.println("i = " + i);
                        i--;
                    } catch (NumberFormatException n) {
                        System.out.println("Item " + element[1].toString() + " deu NumberFormatException");
                        n.printStackTrace();
                    } catch (IOException io) {
                        try {
                            Thread.sleep((long) (Math.random() * 1500));
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MPI_Java.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        i--;
//                        System.out.println("Item " + element[1].toString() + " deu IOException");
                    } catch (MPIException iolp) {
                        System.out.println("Item " + element[1].toString() + " deu MPIException");
                        iolp.printStackTrace();
                    } catch (Exception el) {
                        el.printStackTrace();
                        System.out.println("Item " + element[1].toString() + " deu Exception");
                    }
                }
            }
            //Enviar o mapa parcialmente populado
            Object[] resp = new Object[1];
            resp[0] = col;
            MPI.COMM_WORLD.Send(col, 0, 1, MPI.OBJECT, 0, 999);//tag 999 = job finished
        }
        MPI.Finalize();
    }
}
