package com.azhi.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String email;
}
