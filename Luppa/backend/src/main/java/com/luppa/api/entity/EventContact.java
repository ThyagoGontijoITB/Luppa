package com.luppa.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contatoEvento")
public class EventContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idContato")
    private Integer id;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(name = "numero", nullable = false, length = 20)
    private String phone;

    public static EventContact fromUser(User user) {
        EventContact contact = new EventContact();
        contact.email = user.getEmail();
        contact.phone = user.getPhone();
        return contact;
    }
}
