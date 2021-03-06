package github.aq.cryptowatchapiassetfetcher.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import github.aq.cryptowatchapiassetfetcher.model.Coin;
import github.aq.cryptowatchapiassetfetcher.model.Market;

public class CryptowatchApiCrawler {

	static int coinCount = 0;
	public Dumper dumper;

	// This is the list of Main coin (ETH,BTC)
	public List<Coin> coinList = new ArrayList<Coin>();
	
	Map<String, Set<Coin>> coinListByExchange = new HashMap<String, Set<Coin>>(); // exchange and its coin list
	Map<String, Integer> pairsCountByCoin = new HashMap<String, Integer>(); // coin - pairs count
	Map<String, Set<String>> pairsByExchange = new HashMap<String, Set<String>>();
	Map<String, Integer> pairsCountByExchange = new HashMap<String, Integer>(); // exchange - pairs count
	
	//Map<String, Integer> coinByExchangeCount = new HashMap<String, Integer>();
	Map<String, Set<String>> exchangeByCoin = new HashMap<String, Set<String>>(); // coin - exchange list
	Map<String, Integer> exchangesCountByCoin = new HashMap<String, Integer>(); // coin - exchange count
	
	public String ASSETS_URL = "https://api.cryptowat.ch/assets"; // this API will be used for retrieve every coin info

	/**
	 * This method fetches every coin from cryptowat.ch and loads them into a Data
	 * structure useful for further analysis.The application populates the data for every coin:
	 * relative to the: Name, link for get price
	 * 
	 * @throws MalformedURLException
	 */
	public void crawl() throws MalformedURLException {
		this.getAllCoins();
		for (Coin coin : coinList) {
			if (!coin.isFiat()) {
				String url = ASSETS_URL + "/" + coin.getSymbol();
				JsonElement assetsList = new JsonObject();
				// Filter only the result
				assetsList = getJSON(url).get("result");
				JsonElement assets = new JsonObject();
				assets = assetsList.getAsJsonObject().get("markets").getAsJsonObject().get("base");
				if (assets != null && assets.getAsJsonArray() != null && assets.getAsJsonArray().size() >= 1) {
					for (JsonElement element : assets.getAsJsonArray()) {
						String marketName = element.getAsJsonObject().get("exchange").getAsString();
						String coinName = element.getAsJsonObject().get("pair").getAsString();
						String urlTrade = element.getAsJsonObject().get("route").getAsString();
						Market market = new Market(marketName, coinName, urlTrade, getPrice(urlTrade));
						coin.addMarket(market);
						
						//String exchangeName = parseExchangeName(urlTrade);
						collectCoinByExchange(coin, marketName);
						collectExchangeByCoin(coin, marketName);
						collectMarketPairsByExchange(urlTrade);
						
					}
					if (!coinListByExchange.containsKey(coin.getName())) {
						if (coin.getMarket() != null) 
							pairsCountByCoin.put(coin.getName(), coin.getMarket().size());
					} else {
						if (coin.getMarket() != null) 
							pairsCountByCoin.put(coin.getName(), pairsCountByCoin.get(coin.getName()) + coin.getMarket().size());
					}
				}
				
				System.out.println(coin.toString());
				Dumper.dumpToFile(coin.toString(), "storage/cryptowatch-assets-" + coin.getName().toLowerCase() + ".json");
			}
			for (Map.Entry<String, Set<String>> entry: exchangeByCoin.entrySet()){
				exchangesCountByCoin.put(entry.getKey(), entry.getValue().size());
			}
			
			
		}
		for (Map.Entry<String, Set<String>> entry: pairsByExchange.entrySet()){
			pairsCountByExchange.put(entry.getKey(), entry.getValue().size());
		}
		
		for (Coin coin : coinList) {
			System.out.println(coin.toString());
		}
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String assetListInJson = gson.toJson(coinList);
		Dumper.dumpToFile(assetListInJson, "storage/stats/cryptowatch-assets-list.json");
		System.out.println("coin count: "+coinCount);
		
		// number of coins by exchange
		String jsonCoinListByExchange = gson.toJson(coinListByExchange);
		Dumper.dumpToFile(jsonCoinListByExchange, "storage/stats/cryptowatch-assets-by-exchange.json");
		
		String jsonExchangesByCoin = gson.toJson(exchangeByCoin);
		Dumper.dumpToFile(jsonExchangesByCoin, "storage/stats/cryptowatch-exchanges-by-coins.json");

	    String jsonExchangesCountByCoin = gson.toJson(exchangesCountByCoin);
	    Dumper.dumpToFile(jsonExchangesCountByCoin, "storage/stats/cryptowatch-assets-by-exchange-count.json");
		
		String jsonPairCountByExchange = gson.toJson(pairsCountByExchange);
		Dumper.dumpToFile(jsonPairCountByExchange, "storage/stats/cryptowatch-exchanges-pairs-count.json");
		
		String jsonPairCountByCoin = gson.toJson(pairsCountByCoin);
		Dumper.dumpToFile(jsonPairCountByCoin, "storage/stats/cryptowatch-coin-pairs-count.json");

	}

	private void collectMarketPairsByExchange(String urlTrade) {
		// TODO Auto-generated method stub
		String exchangeName = parseExchangeName(urlTrade);
		// api../markets/bitstamp/zza
		String pair = urlTrade.split("markets")[1].split("/")[2];
		
		if (!pairsByExchange.containsKey(exchangeName)) {
			Set<String> pairSet = new HashSet<String>();
			pairSet.add(pair);
			pairsByExchange.put(exchangeName, pairSet);
		} else {
			Set<String> pairSet = pairsByExchange.get(exchangeName);
			pairSet.add(pair);
			pairsByExchange.put(exchangeName, pairSet);
		}
		
	}

	private void collectExchangeByCoin(Coin coin, String exchangeName) {
		// TODO Auto-generated method stub
		if (!exchangeByCoin.containsKey(coin.getName())) {
			Set<String> exchanges = new HashSet<String>();
			exchanges.add(exchangeName);
			exchangeByCoin.put(coin.getName(), exchanges);
			//coinByExchangeCount.put(coin.getName(), 1);
		} else {
			Set<String> set = exchangeByCoin.get(coin.getName());
			set.add(exchangeName);
			exchangeByCoin.put(coin.getName(), set);
			//coinByExchangeCount.put(coin.getName(), coinByExchangeCount.get(coin.getName()) + 1);
		}
	}

	private String parseExchangeName(String urlTrade) {
		Pattern pattern = Pattern.compile("(?<=markets\\/)(.*?)\\/");
		Matcher matcher = pattern.matcher(urlTrade);
		String exchangeName = "unknown";
		if (matcher.find()) {
			exchangeName = matcher.group();
		}
		return exchangeName;
	}

	private void collectCoinByExchange(Coin coin, String exchangeName) {

		if (!coinListByExchange.containsKey(exchangeName)) {
			Set<Coin> coins = new HashSet<Coin>();
			//coin.setMarket(null);
			coins.add(coin);
			coinListByExchange.put(exchangeName, coins);
			//if (coin.getMarket() != null) {
			//	pairsCountByExchange.put(exchangeName, coin.getMarket().size());
			//}
		} else {
			Set<Coin> set = coinListByExchange.get(exchangeName);
			//coin.setMarket(null);
			set.add(coin);
			coinListByExchange.put(exchangeName, set);
			//if (coin.getMarket() != null) {
				//pairsCountByExchange.put(exchangeName, pairsCountByExchange.get(exchangeName) + coin.getMarket().size());
			//}
		}
		
	}

	/**
	 * This method loads every coin in the myCoin List and is used for loading every coin
	 * name for further analysis
	 * 
	 * @param URL
	 *            - String of the json url API
	 * @return JsonObject - Response of the API
	 * @throws MalformedURLException
	 */

	public void getAllCoins() throws MalformedURLException {
		//coinList = new ArrayList<Coin>();
		JsonElement assetsList = new JsonObject();
		assetsList = getJSON(ASSETS_URL).get("result").getAsJsonArray();
		for (JsonElement element : assetsList.getAsJsonArray()) {
			String symbol = element.getAsJsonObject().get("symbol").getAsString();
			String name = element.getAsJsonObject().get("name").getAsString();
			boolean fiat = Boolean.valueOf(element.getAsJsonObject().get("fiat").getAsBoolean());
			coinList.add(new Coin(name, symbol, fiat));
			coinCount ++;
		}

		for (Coin coin : coinList) {
			System.out.println(coin.toString());
		}

	}

	public float getPrice(String urlMarket) throws MalformedURLException {
		String url = urlMarket + "/price";
		JsonElement element = new JsonObject();
		element = getJSON(url).get("result");
		String price = element.getAsJsonObject().get("price").getAsString();
		return Float.valueOf(price);
	}

	public JsonObject getJSON(String URL) throws MalformedURLException {
		URL url = new URL(URL);
		HttpURLConnection request = null;

		try {
			request = (HttpURLConnection) url.openConnection();
			request.connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Convert to a JSON object to print data
		JsonParser jp = new JsonParser(); // from gson
		JsonElement root = null;
		try {
			root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
		} catch (IOException e) {
			e.printStackTrace();
		} // Convert the input stream to a json element
		JsonObject oggetto = root.getAsJsonObject();
		return oggetto;
	}
	

}
