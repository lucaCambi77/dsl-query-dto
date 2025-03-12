package org.example.dos;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class OneToManyDoClass {

    @Id
    private String id;

    private int intType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", nullable = false, insertable = false, updatable = false)
    private DoClass doClass;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public DoClass getDoClass() {
        return doClass;
    }

    public void setDoClass(DoClass doClass) {
        this.doClass = doClass;
    }
}
