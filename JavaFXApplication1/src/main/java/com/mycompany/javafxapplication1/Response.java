/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */
public class Response {
    private String status;
    private String message;

    public String getStatus() { return status; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "Response{status='" + status + "', message='" + message + "'}";
    }
}
