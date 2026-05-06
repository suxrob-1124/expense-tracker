package com.company.expensetracker.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Column(name = "icon", nullable = false, length = 32)
    private String icon;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected Category() {}

    public Category(String name, String color, String icon, UUID userId) {
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public String getIcon() { return icon; }
    public UUID getUserId() { return userId; }

    public void setName(String name) { this.name = name; }
    public void setColor(String color) { this.color = color; }
    public void setIcon(String icon) { this.icon = icon; }
}
