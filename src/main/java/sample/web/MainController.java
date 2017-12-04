/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.web;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import sample.annotation.AuthorizedClient;

import java.util.Collections;
import java.util.Map;

/**
 * @author Joe Grandja
 */
@Controller
public class MainController {

	@GetMapping("/")
	public String index(@AuthorizedClient("okta") OAuth2AuthorizedClient authorizedClient, Model model) {
		model.addAttribute("userName", authorizedClient.getPrincipalName());
		model.addAttribute("clientName", authorizedClient.getClientRegistration().getClientName());
		return "index";
	}

	@GetMapping("/userinfo")
	public String userinfo(@AuthorizedClient("okta") OAuth2AuthorizedClient authorizedClient, Model model) {
		Map userAttributes = Collections.emptyMap();
		String userInfoEndpointUri = authorizedClient.getClientRegistration()
			.getProviderDetails().getUserInfoEndpoint().getUri();
		if (!StringUtils.isEmpty(userInfoEndpointUri)) {	// userInfoEndpointUri is optional for OIDC Clients
			userAttributes = WebClient.builder()
				.filter(oauth2Credentials(authorizedClient))
				.build()
				.get()
				.uri(userInfoEndpointUri)
				.retrieve()
				.bodyToMono(Map.class)
				.block();
		}
		model.addAttribute("userAttributes", userAttributes);
		return "userinfo";
	}

	private ExchangeFilterFunction oauth2Credentials(OAuth2AuthorizedClient authorizedClient) {
		return ExchangeFilterFunction.ofRequestProcessor(
			clientRequest -> {
				ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + authorizedClient.getAccessToken().getTokenValue())
					.build();
				return Mono.just(authorizedRequest);
			});
	}
}
