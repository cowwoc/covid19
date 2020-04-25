package com.holdmyspot.covid19;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class Main
{
	public static void main(String[] args) throws IOException, ParseException
	{
		Document document = Jsoup.connect("https://www.worldometers.info/coronavirus/").get();
		Elements countryNames = document.select(".mt_a");
		NumberFormat numberFormat = NumberFormat.getInstance();
		Map<State, String> stateToName = new TreeMap<>();
//		Set<String> westernCountries = getWesternCountries();
		Set<String> names = new HashSet<>();
		for (Element nameElement : countryNames)
		{
			String name = nameElement.text();
			if (!names.add(name))
			{
				// Don't process the same country twice
				continue;
			}
//			if (!westernCountries.remove(name))
//			{
//				// Cross off entries from the list
//				continue;
//			}
			Element country = nameElement.parent().parent();
			String totalCasesAsString = country.selectFirst("td:nth-child(2)").text();
			String totalDeathsAsString = country.selectFirst("td:nth-child(4)").text();
			String totalTestsAsString = country.selectFirst("td:nth-child(11)").text();
			String testsPerMillionAsString = country.selectFirst("td:nth-child(12)").text();
			if (totalCasesAsString.isEmpty() || totalDeathsAsString.isEmpty() || testsPerMillionAsString.isEmpty())
				continue;
			int totalCases = numberFormat.parse(totalCasesAsString).intValue();
			int totalDeaths = numberFormat.parse(totalDeathsAsString).intValue();
			int totalTests = numberFormat.parse(totalTestsAsString).intValue();
			int testsPerMillion = numberFormat.parse(testsPerMillionAsString).intValue();
			if (testsPerMillion < 7000)
				continue;
			double caseFatalityRate = totalDeaths / (double) totalCases;

			double totalPopulation = (totalTests / (double) testsPerMillion) * 1_000_000;
			if (totalPopulation < 6_000_000)
				continue;
			double infectionRate = totalCases / (double) totalTests;
			double totalInfection = totalPopulation * infectionRate;
			double mortalityRate = totalDeaths / totalInfection;
			stateToName.put(new State(caseFatalityRate, mortalityRate), name);
		}
		int maxName = stateToName.values().stream().map(String::length).max(Comparator.naturalOrder()).orElse(0);

		int i = 1;
		System.out.println("| Rank | " + " ".repeat(maxName - "Name".length()) + "Name | Mortality Rate (%) | " +
			"Case Fatality Rate (%) | ");
		System.out.println("-".repeat(57 + maxName));
		for (Entry<State, String> entry : stateToName.entrySet())
		{
			State state = entry.getKey();
			String name = entry.getValue();
			System.out.printf("| %4d | %" + maxName + "s | %18.3f | %22.2f | \n", i, name,
				toPercent(state.mortalityRate()), toPercent(state.caseFatalityRate()));
			++i;
		}
//		for (String name : westernCountries)
//			System.err.println("Missing data for: " + name);
	}

	private static Set<String> getWesternCountries() throws IOException
	{
		Document document = Jsoup.connect("https://worldpopulationreview.com/countries/western-countries/").get();
		Set<String> result = new HashSet<>();
		for (Element element : document.select("td a"))
		{
			String name = element.text();
			// Convert names to format expected by worldometers.info
			name = switch (name)
				{
					case "United States" -> "USA";
					case "Trinidad And Tobago" -> "Trinidad and Tobago";
					case "United Kingdom" -> "UK";
					case "Czech Republic" -> "Czechia";
					case "Antigua And Barbuda" -> "Antigua and Barbuda";
					default -> name;
				};
			result.add(name);
		}
		return result;
	}

	/**
	 * Converts a fraction to a percent.
	 *
	 * @param value a double value
	 * @return the value as a percent
	 */
	private static double toPercent(double value)
	{
		return Math.round(value * 100 * 1000) / 1000.0;
	}

	public record State(double caseFatalityRate, double mortalityRate) implements Comparable<State>
	{
		@Override
		public int compareTo(State other)
		{
			return Double.compare(mortalityRate, other.mortalityRate);
		}
	}
}
