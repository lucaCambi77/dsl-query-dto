package org.example;

import org.example.employee.Client;
import org.example.employee.Department;
import org.example.employee.Project;
import org.example.query.WhereCondition;
import org.example.query.QueryField;

public class EmployeeFilterRequest {

  @QueryField(value = "name")
  private String name;

  @QueryField(
      value = "name",
      joinPath = {
        @WhereCondition(collection = "projects", alias = "project", value = Project.class),
        @WhereCondition(alias = "client", value = Client.class)
      })
  private String clientName;

  @QueryField(
      value = "name",
      joinPath = {@WhereCondition(alias = "project", collection = "projects", value = Project.class)})
  private String projectName;

  @QueryField(
      value = "name",
      joinPath = {@WhereCondition(alias = "department", value = Department.class)})
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
