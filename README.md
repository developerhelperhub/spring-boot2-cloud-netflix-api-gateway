# Spring Boot 2.2.5 Cloud Netflix Zuul API Gateway

This repository contains the spring boot cloud netflix api gateway service which is zuul. This example is continuation of the [Cloud Discovery Service](https://github.com/developerhelperhub/spring-boot2-cloud-discovery-and-client) example. I would suggest, please look previous implementation before looking this source code. In this example, I added the three services are ```api-gateway-service```, ```inventory-service``` and ```sales-service```. 

This repository contains seven maven project. 
* my-cloud-service: Its main module, it contains the dependecy management of our application.
* my-cloud-discovery-service: This is the server for the discovery service.
* identity-service: This authentication server service. 
* client-application-service: This client application for authentication server.
* inventory-service: This is one of the microservice which is called inventory service to manage the inventory in the project.
* sales-service: This is one of the microservice which is called sales service to manage the point of sales in the project.

We need to create two microservices are ```inventory-service``` and ```sales-service``` before creating the API gateway. 

### Adding the inventory-service
We create the ```inventory-service``` as resource server under the ```identity-service``` and client service under the ```my-cloud-discovery-service```. 

In the main class InventoryServiceApplication is enabling discovery client:
```java
package com.developerhelperhub.ms.id;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

}
```

In the resource server configuration ```ResourceServerConfig```, we are using the same code of previous example, but only change is that, we have to maintain the unique resoure id for this service and authority of the endpoints. Below I added codes only the changes in the previous example.
```java

        private static final String RESOURCE_ID = "inventory_service_resource_id";
	
	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.anonymous().disable().authorizeRequests().antMatchers("/**").access("hasRole('USER')").and()
				.exceptionHandling().accessDeniedHandler(new OAuth2AccessDeniedHandler());
	}
```

Added the new controller class is InventoryController:
```java
package com.developerhelperhub.ms.id.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventoryController {

	@RequestMapping(value = "/items", method = RequestMethod.GET)
	public String items() {
		return "list of items";
	}

	@RequestMapping(value = "/items", method = RequestMethod.POST)
	public String addItem() {
		return "added item";
	}

	@RequestMapping(value = "/items/{id}", method = RequestMethod.GET)
	public String getItem(@PathVariable(value = "id") Long id) {
		return "get item by " + id;
	}
}
```

In the YAML file we have change on the application name and port
```yml
spring:
  application:
    name: inventory-service

server:
  port: 8084

```

*Note:* When we will create each new microservice is mandatory as resource server as well as discovery client. 

### Adding the sales-service
In the same code changes of ```inventory-service``` are using in the ```sales-service```. So I am just adding the code only in this service.

In the resource server configuration ```ResourceServerConfig```:
```java

        private static final String RESOURCE_ID = "sales_service_resource_id";
	
	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.anonymous().disable().authorizeRequests().antMatchers("/**").access("hasRole('USER')").and()
				.exceptionHandling().accessDeniedHandler(new OAuth2AccessDeniedHandler());
	}
```

Added the new controller class is InventoryController:
```java
package com.developerhelperhub.ms.id.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SalesController {

	@RequestMapping(value = "/items", method = RequestMethod.GET)
	public String items() {
		return "list of items";
	}

	@RequestMapping(value = "/items", method = RequestMethod.POST)
	public String addItem() {
		return "added item";
	}

	@RequestMapping(value = "/items/{id}", method = RequestMethod.GET)
	public String getItem(@PathVariable(value = "id") Long id) {
		return "get item by " + id;
	}
}
```

In the YAML file we have change on the application name and port
```yml
spring:
  application:
    name: sales-service

server:
  port: 8083

```

### Adding the api-gateway-service
This service is the API gateway service of all microservice services and its main service to communicate from the clients.

This discovery serivce is running on ```8085``` port.

We need to add below service dependency in the ```pom.xml```.

```xml
<dependency>
     <groupId>org.springframework.cloud</groupId>
     <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
</dependency>
```

Main class ApiGatewayServiceApplication:
```java
package com.developerhelperhub.ms.id;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
@EnableDiscoveryClient
@EnableZuulProxy
public class ApiGatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayServiceApplication.class, args);
	}

}
```

We need to add the ```@EnableZuulProxy``` annotation for enabling the zuul, here I am using ```@EnableZuulProxy``` because when we are using the discovery service, we need to use the zuul proxy instead of ```@EnableZuulServer```. In the zuul configuration we can use the spring application name instead of URL and port of services.

In the resource server configuration ```ResourceServerConfig```, we are using the same code of previous example, but only change in the unique resoure id.
```java
        private static final String RESOURCE_ID = "api_gateway_resource_id";
```

We need to add the below configuration in the YAML file
```yml
spring:
  application:
    name: api-gateway-service

logging:
  level:
    org:
      springframework: DEBUG

server:
  port: 8085

eureka:
  client:
    serviceUrl:
      defaultZone: http://discovery:discoverypass@localhost:8761/eureka/
      
zuul:
  routes:
    inventory:
      path: /inventory/**
      sensitiveHeaders: Cookie,Set-Cookie
      serviceId: inventory-service
    sales:
      path: /sales/**
      sensitiveHeaders: Cookie,Set-Cookie
      serviceId: sales-service
```
In the zuul configuraiton we need to add the each route for the respective microservice and its pattern in the URL to routing the request to respective microservice. We need to remove the Authorization in the sensitive header because zuul default values are ```Cookie,Set-Cookie,Authroization```. Otherwise authroization header will not down stream to microservice.

### Changes on identity-service
We need to add the resource server ids and user role in the client registration in the ```IdentityServiceApplication``` main class. 

```java
        public void run(String... args) throws Exception {
		user.setUsername("mycloud");
		user.setPassword("mycloud@1234");
		user.setAccountNonExpired(true);
		user.setAccountNonLocked(true);
		user.setCredentialsNonExpired(true);
		user.setEnabled(true);

		// Added the user role
		user.addGrantedAuthority("ADMIN");
		user.addGrantedAuthority("USER");

		user.create();

		client.setClientId("my-cloud-identity");
		client.setClientSecret("VkZpzzKa3uMq4vqg");

		// Added the new resources id's
		client.setResourceIds(new HashSet<String>(Arrays.asList("identity_id", "inventory_service_resource_id",
				"api_gateway_resource_id", "sales_service_resource_id", "my_cloud_discovery_id")));

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

	}
```

### Testing the implementation
We need to run all services and invoke the endpoint the postman.

* We can look the [example](https://github.com/developerhelperhub/spring-boot2-oauth2-clients-users-from-db#to-generate-the-tokens-with-grant-type-password) how to generate the token.
* ```localhost:8085/sales/items``` end point can be used to get the list items in the sales service.
* ```localhost:8085/inventory/items``` end point can be used to get the list items in the inventory service.

### Reference
* [Netflix Zuul Documentation](https://cloud.spring.io/spring-cloud-netflix/2.2.x/reference/html/#router-and-filter-zuul)
* [Handle the 401 error in the zuul service](https://stackoverflow.com/questions/38936470/spring-cloud-zuul-gateway-401-basic-authentication)
