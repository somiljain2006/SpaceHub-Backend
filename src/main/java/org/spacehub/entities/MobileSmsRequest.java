package org.spacehub.entities;

import lombok.Data;

@Data
public class MobileSmsRequest {

    private final String mobileNumber;
    private final String message;

}
