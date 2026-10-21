package com.alea.pokemon.adapter.out.pokeapi;

import com.alea.pokemon.domain.Pokemon;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokeApiClientTest {

    @RegisterExtension
    static WireMockExtension pokeApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private PokeApiClient client;

    @BeforeEach
    void setUp() {
        PokeApiProperties properties = new PokeApiProperties(
                pokeApi.baseUrl(), Duration.ofSeconds(2), Duration.ofSeconds(2), 3, Duration.ZERO, 10);
        client = new PokeApiClient(new PokeApiConfig().pokeApiRestClient(properties));
    }

    @Test
    void listPokemonUrls_returnsTheUrlOfEveryPokemon() {
        pokeApi.stubFor(get(urlEqualTo("/pokemon?limit=100000"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "count": 2,
                                  "next": null,
                                  "previous": null,
                                  "results": [
                                    {"name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon/1/"},
                                    {"name": "ivysaur", "url": "https://pokeapi.co/api/v2/pokemon/2/"}
                                  ]
                                }
                                """)));

        List<String> urls = client.listPokemonUrls();

        assertThat(urls).containsExactly(
                "https://pokeapi.co/api/v2/pokemon/1/",
                "https://pokeapi.co/api/v2/pokemon/2/");
    }

    @Test
    void fetchPokemon_mapsTheJsonToADomainPokemon() {
        pokeApi.stubFor(get(urlEqualTo("/pokemon/25/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 25,
                                  "name": "pikachu",
                                  "base_experience": 112,
                                  "height": 4,
                                  "weight": 60,
                                  "is_default": true,
                                  "abilities": [{"slot": 1}]
                                }
                                """)));

        Pokemon pokemon = client.fetchPokemon(pokeApi.baseUrl() + "/pokemon/25/");

        assertThat(pokemon).isEqualTo(Pokemon.builder()
                .id(25).name("pikachu").weight(60).height(4).baseExperience(112).build());
    }

    @Test
    void fetchPokemon_withNullBaseExperience_usesZero() {
        pokeApi.stubFor(get(urlEqualTo("/pokemon/10100/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": 10100, "name": "some-form", "base_experience": null, "height": 3, "weight": 30}
                                """)));

        Pokemon pokemon = client.fetchPokemon(pokeApi.baseUrl() + "/pokemon/10100/");

        assertThat(pokemon.getBaseExperience()).isZero();
    }

    @Test
    void fetchPokemon_whenPokeApiAnswersWithAnError_throws() {
        pokeApi.stubFor(get(urlEqualTo("/pokemon/500/")).willReturn(aResponse().withStatus(500)));
        pokeApi.stubFor(get(urlEqualTo("/pokemon/404/")).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client.fetchPokemon(pokeApi.baseUrl() + "/pokemon/500/"))
                .isInstanceOf(RestClientException.class);
        assertThatThrownBy(() -> client.fetchPokemon(pokeApi.baseUrl() + "/pokemon/404/"))
                .isInstanceOf(RestClientException.class);
    }
}
