package org.example.dos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
public class DoClass {

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "doClass")
    private Set<OneToManyDoClass> oneToManyObjects = new HashSet<>();

    @Id
    @Column(name = "id", unique = true, nullable = false, length = 36)
    private String id;

    private long longType;

    private float floatType;

    private short shortType;

    private Date dateType;

    private LocalDate localDateType;

    private LocalDateTime localDateTimeType;

    private BigDecimal decimalType;

    private BigInteger bigIntegerType;

    private Double doubleType;

    private int intType;

    private boolean booleanType;

    @Enumerated(EnumType.STRING)
    private StatusEnumDO status;

    public Set<OneToManyDoClass> oneToManyRelation() {
        return oneToManyObjects;
    }

    public void setOneToManyRelation(Set<OneToManyDoClass> oneToManyObjects) {
        this.oneToManyObjects = oneToManyObjects;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
