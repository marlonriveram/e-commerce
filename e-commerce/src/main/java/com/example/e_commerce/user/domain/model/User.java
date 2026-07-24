package com.example.e_commerce.user.domain.model;

import com.example.e_commerce.user.domain.enums.EnumRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    private Long id;
    private String name;
    private String email;
    private EnumRole role;
}
