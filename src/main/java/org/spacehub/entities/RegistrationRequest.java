package org.spacehub.entities;

public record RegistrationRequest(String firstName,
                                  String lastName, String email, String password) {
}
