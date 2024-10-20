package org.example.query;

import static org.example.employee.Employee.PROJECTS;

import org.example.employee.Client;
import org.example.employee.Department;
import org.example.employee.ProjectToDo;
import org.example.query.annnotation.JOIN;
import org.example.query.annnotation.QueryField;

public class FilterRequest {

  @QueryField(fieldName = "name")
  private String name;

  @QueryField(
      fieldName = "name",
      joinPath = {
        @JOIN(collection = PROJECTS, entityClass = ProjectToDo.class),
        @JOIN(entityClass = Client.class)
      })
  private String clientName;

  @QueryField(
      fieldName = "name",
      joinPath = {@JOIN(collection = PROJECTS, entityClass = ProjectToDo.class)})
  private String projectName;

  @QueryField(
      fieldName = "name",
      joinPath = {@JOIN(entityClass = Department.class)})
  private String departmentName;

  public void setDepartmentName(String departmentName) {
    this.departmentName = departmentName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  public String getName() {
    return name;
  }

  public String getClientName() {
    return clientName;
  }

  public String getProjectName() {
    return projectName;
  }

  public String getDepartmentName() {
    return departmentName;
  }
}
