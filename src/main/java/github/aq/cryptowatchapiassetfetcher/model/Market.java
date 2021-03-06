package github.aq.cryptowatchapiassetfetcher.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * @author  
 * 
 *         This is the Java representation of a Market: A Market has a
 *         Name, an URL, an a list of Coin instances.
 *
 */

public class Market {

	public String marketName; // This is the common name for the exchange(bitrex,gdax)ecc
	public String coinName; // adaeth, adabtc
	public String urlTrade; // gdax/btc-eur
	public float price;

	public Market() {

	}

	public Market(String marketName, String coinName, String urlTrade, float price) {
		super();
		this.marketName = marketName;
		this.coinName = coinName;
		this.urlTrade = urlTrade;
		this.price = price;
	}

	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this);
	}

	public String getMarketName() {
		return marketName;
	}

	public void setMarketName(String marketName) {
		this.marketName = marketName;
	}

	public String getCoinName() {
		return coinName;
	}

	public void setCoinName(String coinName) {
		this.coinName = coinName;
	}

	public String getUrlTrade() {
		return urlTrade;
	}

	public void setUrlTrade(String urlTrade) {
		this.urlTrade = urlTrade;
	}

	public String getName() {
		return marketName;
	}

	public void setName(String name) {
		this.marketName = name;
	}

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}
}
