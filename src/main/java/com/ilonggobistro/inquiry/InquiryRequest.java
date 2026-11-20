package com.ilonggobistro.inquiry;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InquiryRequest {

    @NotBlank(message = "Please enter your name")
    @Size(max = 120)
    private String name;

    @Size(max = 30)
    private String phone;

    @NotBlank(message = "Please enter your email")
    @Email(message = "Enter a valid email address")
    @Size(max = 160)
    private String email;

    @NotBlank(message = "Please pick an inquiry topic")
    private String inquiryType = "General Question";

    @NotBlank(message = "Please share your message")
    @Size(max = 2000, message = "Message must be 2000 characters or fewer")
    private String message;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInquiryType() {
        return inquiryType;
    }

    public void setInquiryType(String inquiryType) {
        this.inquiryType = inquiryType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
