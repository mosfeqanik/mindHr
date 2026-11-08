package com.mindhr.entity;

import com.mindhr.entity.base.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "menu_categories", uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "name"}))
public class MenuCategory extends Tenant {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "category")
    private List<MenuItem> items = new ArrayList<>();
}
