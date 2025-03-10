import org.example.query.annnotation.FilterRequest;
import org.example.query.annnotation.JOIN;
import org.example.query.annnotation.QueryField;

import java.util.List;

import static org.example.model.Employee.CLIENT;
import static org.example.model.Employee.DEPARTMENT;
import static org.example.model.Employee.PROJECTS;

public class FilterRequestTest implements FilterRequest {

    private List<String> name;

    @QueryField(
            name = "name",
            joinPath = {
                    @JOIN(entityToJoin = PROJECTS),
                    @JOIN(entityToJoin = CLIENT)
            })
    private List<String> clientName;

    @QueryField(
            name = "name",
            joinPath = {@JOIN(entityToJoin = PROJECTS)})
    private List<String> projectName;

    @QueryField(
            name = "name",
            joinPath = {@JOIN(entityToJoin = DEPARTMENT)})
    private List<String> departmentName;

    public void setDepartmentName(List<String> departmentName) {
        this.departmentName = departmentName;
    }

    public void setProjectName(List<String> projectName) {
        this.projectName = projectName;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public void setClientName(List<String> clientName) {
        this.clientName = clientName;
    }
}
