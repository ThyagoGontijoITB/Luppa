package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "endereco")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idEndereco")
    private Integer id;

    @Column(name = "cep", nullable = false, length = 12)
    private String zipCode = "00000000";

    @Column(name = "rua", nullable = false, length = 100)
    private String street = "Nao informado";

    @Column(name = "cidade", nullable = false, length = 40)
    private String city = "Nao informado";

    @Column(name = "estado", nullable = false, length = 30)
    private String state = "Nao informado";

    @Column(name = "pais", nullable = false, length = 40)
    private String country = "Brasil";

    @Column(name = "numero", nullable = false)
    private Integer number = 0;

    public String display() {
        return String.join(", ", streetWithNumber(), city, state, country);
    }

    public static Address fromLocation(String location) {
        Address address = new Address();
        if (location != null && !location.isBlank()) {
            address.street = truncate(location.trim(), 100);
        }
        return address;
    }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = truncate(defaultString(zipCode, "00000000"), 12); }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = truncate(defaultString(street, "Nao informado"), 100); }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = truncate(defaultString(city, "Nao informado"), 40); }
    public String getState() { return state; }
    public void setState(String state) { this.state = truncate(defaultString(state, "Nao informado"), 30); }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = truncate(defaultString(country, "Brasil"), 40); }
    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number == null ? 0 : number; }

    private String streetWithNumber() {
        return number == null || number == 0 ? street : street + ", " + number;
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
