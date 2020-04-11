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
