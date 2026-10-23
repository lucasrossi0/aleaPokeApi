package com.alea.pokemon;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PokemonRankingsIntegrationTest {

    private static final String LIST_PATH = "/pokemon?limit=100000";

    private static final WireMockServer pokeApi = startFakePokeApi();

    @DynamicPropertySource
    static void pokeApiProperties(DynamicPropertyRegistry registry) {
        registry.add("pokeapi.base-url", pokeApi::baseUrl);
        registry.add("pokeapi.max-attempts", () -> 3);
        registry.add("pokeapi.retry-delay", () -> "0ms");
    }

    @AfterAll
    static void stopFakePokeApi() {
        pokeApi.stop();
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void whileLoading_answers503_andWhenLoaded_answers200() throws Exception {
        mockMvc.perform(get("/api/pokemon/heaviest"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Not Ready"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable());

        waitUntilRankingsAreReady();

        mockMvc.perform(get("/api/pokemon/heaviest"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(2)
    void heaviest_returnsTheFiveHeaviestPokemon() throws Exception {
        waitUntilRankingsAreReady();

        mockMvc.perform(get("/api/pokemon/heaviest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("snorlax", "wailord", "onix", "venusaur", "charizard")))
                .andExpect(jsonPath("$[0].id").value(143))
                .andExpect(jsonPath("$[0].weight").value(4600))
                .andExpect(jsonPath("$[0].height").value(21))
                .andExpect(jsonPath("$[0].baseExperience").value(189));
    }

    @Test
    @Order(3)
    void highest_returnsTheFiveHighestPokemon() throws Exception {
        waitUntilRankingsAreReady();

        mockMvc.perform(get("/api/pokemon/highest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("wailord", "onix", "snorlax", "venusaur", "charizard")))
                .andExpect(jsonPath("$[0].height").value(145));
    }

    @Test
    @Order(4)
    void baseExperience_returnsTheFiveWithMostBaseExperience() throws Exception {
        waitUntilRankingsAreReady();

        mockMvc.perform(get("/api/pokemon/base-experience"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("charizard", "venusaur", "snorlax", "wailord", "ivysaur")))
                .andExpect(jsonPath("$[0].baseExperience").value(267));
    }

    @Test
    @Order(5)
    void requests_neverCallPokeApiAgain_andRetriesHappenedOnlyWhereNeeded() throws Exception {
        waitUntilRankingsAreReady();

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/pokemon/heaviest")).andExpect(status().isOk());
            mockMvc.perform(get("/api/pokemon/highest")).andExpect(status().isOk());
            mockMvc.perform(get("/api/pokemon/base-experience")).andExpect(status().isOk());
        }

        pokeApi.verify(1, getRequestedFor(urlEqualTo(LIST_PATH)));
        pokeApi.verify(1, getRequestedFor(urlEqualTo("/pokemon/143/")));  // snorlax: worked at the first attempt
        pokeApi.verify(2, getRequestedFor(urlEqualTo("/pokemon/7/")));    // squirtle: failed once, worked on retry
        pokeApi.verify(3, getRequestedFor(urlEqualTo("/pokemon/890/")));  // eternatus: 3 attempts, then skipped
    }

    private void waitUntilRankingsAreReady() {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mockMvc.perform(get("/api/pokemon/heaviest")).andExpect(status().isOk()));
    }

    private static WireMockServer startFakePokeApi() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();

        server.stubFor(WireMock.get(urlEqualTo(LIST_PATH))
                .willReturn(json(listBody(server.baseUrl())).withFixedDelay(1000)));

        stubPokemon(server, 1, "bulbasaur", 69, 7, "64");
        stubPokemon(server, 2, "ivysaur", 130, 10, "142");
        stubPokemon(server, 3, "venusaur", 1000, 20, "263");
        stubPokemon(server, 6, "charizard", 905, 17, "267");
        stubPokemon(server, 4, "charmander", 85, 6, "62");
        stubPokemon(server, 25, "pikachu", 60, 4, "112");
        stubPokemon(server, 143, "snorlax", 4600, 21, "189");
        stubPokemon(server, 321, "wailord", 3980, 145, "175");
        stubPokemon(server, 95, "onix", 2100, 88, "77");
        stubPokemon(server, 10001, "mystery-form", 100, 3, "null");

        server.stubFor(WireMock.get(urlEqualTo("/pokemon/7/"))
                .inScenario("squirtle").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("second attempt"));
        server.stubFor(WireMock.get(urlEqualTo("/pokemon/7/"))
                .inScenario("squirtle").whenScenarioStateIs("second attempt")
                .willReturn(json(detailBody(7, "squirtle", 90, 5, "63"))));

        server.stubFor(WireMock.get(urlEqualTo("/pokemon/890/"))
                .willReturn(aResponse().withStatus(500)));

        return server;
    }

    private static void stubPokemon(WireMockServer server, int id, String name, int weight, int height, String baseExperience) {
        server.stubFor(WireMock.get(urlEqualTo("/pokemon/" + id + "/"))
                .willReturn(json(detailBody(id, name, weight, height, baseExperience))));
    }

    private static ResponseDefinitionBuilder json(String body) {
        return aResponse().withHeader("Content-Type", "application/json").withBody(body);
    }

    private static String detailBody(int id, String name, int weight, int height, String baseExperience) {
        return """
                {"id": %d, "name": "%s", "weight": %d, "height": %d, "base_experience": %s, "is_default": true}
                """.formatted(id, name, weight, height, baseExperience);
    }

    private static String listBody(String baseUrl) {
        int[] ids = {1, 2, 3, 6, 4, 25, 143, 321, 95, 10001, 7, 890};
        String[] names = {"bulbasaur", "ivysaur", "venusaur", "charizard", "charmander", "pikachu",
                "snorlax", "wailord", "onix", "mystery-form", "squirtle", "eternatus"};
        StringBuilder results = new StringBuilder();
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                results.append(",");
            }
            results.append("""
                    {"name": "%s", "url": "%s/pokemon/%d/"}""".formatted(names[i], baseUrl, ids[i]));
        }
        return "{\"count\": %d, \"next\": null, \"previous\": null, \"results\": [%s]}".formatted(ids.length, results);
    }
}
