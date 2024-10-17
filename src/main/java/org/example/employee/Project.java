package org.example.employee;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
public class Project {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  @ManyToMany(mappedBy = "projects") // This is the reverse side of the relationship
  private Set<Employee> employees = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "client_id", nullable = false)
  private Client client;

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<Employee> getEmployee() {
    return employees;
  }

  public void setEmployee(Set<Employee> employee) {
    this.employees = employee;
  }

  public Client getClient() {
    return client;
  }

  public void setClient(Client client) {
    this.client = client;
  }
}
