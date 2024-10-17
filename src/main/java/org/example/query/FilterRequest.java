package org.example.query;

import org.example.employee.Client;
import org.example.employee.Department;
import org.example.employee.ProjectToDo;
import org.example.query.annnotation.QueryField;
import org.example.query.annnotation.JOIN;

public class FilterRequest {

  @QueryField(fieldName = "name")
  private String name;

  @QueryField(
      fieldName = "name",
      joinPath = {
        @JOIN(collection = "projects", entityClass = ProjectToDo.class),
        @JOIN(entityClass = Client.class)
      })
  private String clientName;

  @QueryField(
      fieldName = "name",
      joinPath = {@JOIN(collection = "projects", entityClass = ProjectToDo.class)})
  private String projectName;

  @QueryField(
      fieldName = "name",
      joinPath = {@JOIN(entityClass = Department.class)})
  private String departmentName;

  public String getDepartmentName() {
    return departmentName;
  }

  public void setDepartmentName(String departmentName) {
    this.departmentName = departmentName;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }
}
