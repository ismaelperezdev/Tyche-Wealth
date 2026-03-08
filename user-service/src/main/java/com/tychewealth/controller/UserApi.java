package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.URL_FOLDER_V1;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = URL_FOLDER_V1 + "/user")
@Tag(name = "User")
public interface UserApi {}
