package org.wikimedia.lsearch.frontend;

import java.util.regex.Matcher;

import org.wikimedia.lsearch.frontend.SearchDaemon;

import static org.junit.Assert.*;

import org.junit.Test;

public class SearchDaemonTest {

	@Test
	public void test() {
		String email = "foo@bar.com";
		String[] credit_cards = new String[8];
		// Credit Card Sample Number
		credit_cards[0] = "4111 1111 1111 1111"; // Visa
		credit_cards[1] = "5500 0000 0000 0004"; // MasterCard
		credit_cards[2] = "3400 0000 0000 009"; // American Express
		credit_cards[3] = "3000 0000 0000 04"; // Diner's Club
		credit_cards[4] = "3000 0000 0000 04"; // Carte Blanche
		credit_cards[5] = "6011 0000 0000 0004"; // Discover
		credit_cards[6] = "2014 0000 0000 009"; // en Route
		credit_cards[7] = "3088 0000 0000 0009"; // JCB

		String ssn = "987-65-4320";
		// Test email regex
		Matcher match = SearchDaemon.logResultFilterPatterns[0].matcher(email);

		// Test credit card regex
		for (int i = 0; i < credit_cards.length; i++) {
			match = SearchDaemon.logResultFilterPatterns[1]
					.matcher(credit_cards[i]);
			assertEquals(match.matches(), true);
		}

		// Test US social security regex
		match = SearchDaemon.logResultFilterPatterns[2].matcher(ssn);
		assertEquals(match.matches(), true);

	}

}
