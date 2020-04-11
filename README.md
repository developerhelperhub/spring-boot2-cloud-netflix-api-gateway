# Spring Boot 2.2.5 Cloud Discovery Server and its Client

This repository contains the spring boot cloud discovery and its client implementation. This example is continuation of the [Oauth2 Autherization and Resource Servers](https://github.com/developerhelperhub/spring-boot2-oauth2-clients-users-from-db) example. I would suggest, please look previous implementation before looking this source code. In this example I registered the ```resource-service``` under the ```my-cloud-discovery-service```. 

This repository contains five maven project. 
* my-cloud-service: Its main module, it contains the dependecy management of our application.
* my-cloud-discovery-service: This is the server for the discovery service.
* identity-service: This authentication server service. 
* client-application-service: This client application for authentication server.
* resource-service: This resource server to provide the resource services for our application.

### Adding the my-cloud-discovery-service
This service is the discovery service of all services and it helps to identify the servics are currently running in which ip address and its port. I am seeing the major advantage of discovery service is that, In the spring boot, we can use the spring boot application name for communicating in other service like "http://resource-service/user" instead of hard coding the ip address. This helps our application can be moved one cloud to another cloud with minimum change. 

This discovery serivce is running on ```8761``` port.

Note: This use case can be solved in different approch, like we can use DNS name instead of ip address.

We need to add below discovery service dependency in the ```pom.xml```. The spring boot is using eureka server to support discovery service in the spring boot applications.

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```
In this application, I am using two type of security once basic http security for enabling the security of the ```/eureka/**``` endpoints and ```/``` eureka dashboard. another once is oauth2 security for accessing of all other resources for this application like ```/clients/**``` and ```"/**```. It means this application is running as resource server under ```identity-service```.

The main application class we need to enable the ```@EnableEurekaServer```.
```java
package com.developerhelperhub.ms.id;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class MyCloudDiscoveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyCloudDiscoveryApplication.class, args);
	}

}
```

I adding the ```ResourceServerConfig``` class for enabling the resource server under the ```identity-service```.
```java
package com.developerhelperhub.ms.id.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Configuration
@Order(1)
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	private static final String RESOURCE_ID = "my_cloud_discovery_id";

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.resourceId(RESOURCE_ID).stateless(true).tokenServices(tokenServices());
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.anonymous().disable().authorizeRequests().antMatchers("/clients/**", "/**").access("hasRole('ADMIN')")
				.and().exceptionHandling().accessDeniedHandler(new OAuth2AccessDeniedHandler());
	}

	@Bean
	public DefaultTokenServices tokenServices() {
		DefaultTokenServices tokenServices = new DefaultTokenServices();
		tokenServices.setTokenStore(tokenStore());
		return tokenServices;
	}

	@Bean
	public JwtAccessTokenConverter accessTokenConverter() {
		JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
		converter.setSigningKey("123456");
		return converter;
	}

	@Bean
	public TokenStore tokenStore() {
		JwtTokenStore store = new JwtTokenStore(accessTokenConverter());
		return store;
	}

}
```
Note: ```my_cloud_discovery_id``` the id should be added in the client registraion in the ```identity-service``` and these resources can be accessed only who are the users have role as ```ADMIN```. 

I am adding the ```SecurityConfiguration``` class for enabling the basic http security for the ```eureka``` end points and dashboard.
```java
package com.developerhelperhub.ms.id.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@Order(2)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.csrf().disable().authorizeRequests().antMatchers("/", "/eureka/**").authenticated().and().httpBasic()
				.authenticationEntryPoint(new AuthenticationEntryPointImpl());
	}

}
```

Another once class is ```AuthenticationEntryPointImpl``` for entry point of basic security failed while loading the dashboard in the browser. While loading the dashboard in the browser, the application ask to enter the username and password of the basic http security. The username and password are ```discovery``` and ```discoverypass```.
```java
package com.developerhelperhub.ms.id.config;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

public class AuthenticationEntryPointImpl extends BasicAuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.addHeader("WWW-Authenticate", "Basic realm=" + getRealmName());
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		PrintWriter writer = response.getWriter();
		writer.println("HTTP Status 401 - " + authException.getMessage());
	}

	@Override
	public void afterPropertiesSet() {
		setRealmName("discovery-service");
		super.afterPropertiesSet();
	}
}
```

I created ```/clients/applications``` endpoint to list the applications which are registered under this discovery service. 
```java
package com.developerhelperhub.ms.id.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.EurekaServerContext;

@RestController
public class ClientController {

	@Autowired
	private EurekaServerContext discoveryClient;

	@RequestMapping(value = "/clients/applications", method = RequestMethod.GET)
	public Applications applications() {
		return discoveryClient.getRegistry().getApplications();
	}

}
```
I added the YAML file to configure the eureka and basic http security

```yml
spring:
  application:
    name: my-cloud-discovery-service
  security:
    user:
      name: discovery
      password: discoverypass

logging:
  level:
    org:
      springframework: DEBUG
    com:
      netflix:
        eureka: OFF
        discovery: OFF

server:
  port: 8761
  

eureka:
  dashboard:
    enabled: true
  client:
    register-with-eureka: false
    fetch-registry: false
```

### Changes in the resource-service
We need to add some code to make the client service under the discovery service.

Add the below dependency in the ```pom.xml```
```xml
<dependency>
     <groupId>org.springframework.cloud</groupId>
     <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
     <groupId>org.springframework.cloud</groupId>
     <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

Add the ```@EnableDiscoveryClient``` annotation in the main class to enable the discovery client for the resource service.
```java
package com.developerhelperhub.ms.id;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ResourceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceServiceApplication.class, args);
	}
}
```

Added the eureka configuration in the YAML file, here we need to specify the username and password of eureka endpoint.
```yml
eureka:
  client:
    serviceUrl:
      defaultZone: http://discovery:discoverypass@localhost:8761/eureka/
```

### Changes on identity-service
We need to add the ```my_cloud_discovery_id``` resource server id in the client registration in the ```IdentityServiceApplication``` main class. 

```java
client.setClientId("my-cloud-identity");
		client.setClientSecret("VkZpzzKa3uMq4vqg");

		client.setResourceIds(
				new HashSet<String>(Arrays.asList("identity_id", "resource_id", "my_cloud_discovery_id")));

		client.addGrantedAuthority("ADMIN");

		client.setSecretRequired(true);
		client.setScoped(true);
		client.setScope(new HashSet<String>(Arrays.asList("user_info")));
		client.setAuthorizedGrantTypes(
				new HashSet<String>(Arrays.asList("authorization_code", "password", "refresh_token")));
		client.setRegisteredRedirectUri(new HashSet<String>(Arrays.asList("http://localhost:8082/login/oauth2/code/")));
		client.setAccessTokenValiditySeconds(43199);
		client.setRefreshTokenValiditySeconds(83199);
		client.setAutoApprove(true);

		client.create();
```

### Testing the implementation

* We can look the [example](https://github.com/developerhelperhub/spring-boot2-oauth2-clients-users-from-db#to-generate-the-tokens-with-grant-type-password) how to generate the token.
* ```localhost:8761/clients/applications``` end point can be used to get the list of applications registered under the discovery service.
* ```http://localhost:8761/``` end point can be used to view the eureka dashboard. We need to enter the username and password to view the dashboard. The username and password are given below.

### Reference
* [Eureka Example](https://spring.io/guides/gs/service-registration-and-discovery/)
* [Basic Authentication Entry Endpoint](https://o7planning.org/en/11649/secure-spring-boot-restful-service-using-basic-authentication)
