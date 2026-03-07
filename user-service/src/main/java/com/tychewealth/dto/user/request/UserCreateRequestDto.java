package com.tychewealth.dto.user.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserCreateRequestDto {

    private String email;
    private String username;
    private String password;
}
