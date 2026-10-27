package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceDaoImpl extends BaseJdbcDao implements ResourceDao {

    private static final String SELECT_RESOURCE = "SELECT r.*, c.nombre_categoria "
            + "FROM recursos r JOIN categorias_recurso c ON c.id_categoria = r.id_categoria ";

    private static final String OPEN_STATES = "('Activo','Vencido')";

    @Override
    public List<ResourceCategory> findCategories() {
        return query("SELECT * FROM categorias_recurso ORDER BY id_categoria", this::mapCategory);
    }

    @Override
    public Optional<ResourceCategory> findCategoryById(int id) {
        return queryOne("SELECT * FROM categorias_recurso WHERE id_categoria = ?", this::mapCategory, id);
    }

    @Override
    public Optional<Resource> findById(int id) {
        return queryOne(SELECT_RESOURCE + "WHERE r.id_recurso = ?", this::mapResource, id);
    }

    @Override
    public Optional<Resource> findByCode(String code) {
        return queryOne(SELECT_RESOURCE + "WHERE r.codigo_recurso = ?", this::mapResource, code);
    }

    @Override
    public List<Resource> findAll(ResourceStatus status) {
        if (status == null) {
            return query(SELECT_RESOURCE + "ORDER BY r.codigo_recurso", this::mapResource);
        }
        return query(SELECT_RESOURCE + "WHERE r.estado_recurso = ? ORDER BY r.codigo_recurso",
                this::mapResource, status.label());
    }

    @Override
    public int insert(Resource r) {
        return insert("INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, "
                        + "fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES (?, ?, ?, ?, ?, ?, ?)",
                r.code(), r.name(), r.categoryId(), r.description(), toSql(r.acquisitionDate()),
                r.location(), r.status().label());
    }

    @Override
    public void updateDetails(Resource r) {
        update("UPDATE recursos SET nombre_recurso = ?, id_categoria = ?, descripcion = ?, "
                        + "fecha_adquisicion = ?, ubicacion_fisica = ? WHERE id_recurso = ?",
                r.name(), r.categoryId(), r.description(), toSql(r.acquisitionDate()), r.location(), r.id());
    }

    @Override
    public void updateStatus(int resourceId, ResourceStatus status) {
        update("UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?", status.label(), resourceId);
    }

    @Override
    public int lend(int resourceId, BorrowerType type, int borrowerId, LocalDate loanDate,
                    LocalDate expectedReturn, String notes) {
        String table = loanTable(type);
        String borrowerColumn = borrowerColumn(type);
        return inTransaction(c -> {
            int changed = update(c, "UPDATE recursos SET estado_recurso = 'Prestado' "
                    + "WHERE id_recurso = ? AND estado_recurso = 'Disponible'", resourceId);
            if (changed == 0) {
                return -1;
            }
            return insert(c, "INSERT INTO " + table + " (id_recurso, " + borrowerColumn + ", fecha_prestamo, "
                            + "fecha_devolucion_esperada, estado_prestamo, observaciones) "
                            + "VALUES (?, ?, ?, ?, 'Activo', ?)",
                    resourceId, borrowerId, toSql(loanDate), toSql(expectedReturn), notes);
        });
    }

    @Override
    public boolean returnLoan(Loan loan, LocalDate returnDate) {
        String table = loanTable(loan.borrowerType());
        return inTransaction(c -> {
            int closed = update(c, "UPDATE " + table + " SET estado_prestamo = 'Devuelto', "
                    + "fecha_devolucion_real = ? WHERE id_prestamo = ? AND estado_prestamo IN " + OPEN_STATES,
                    toSql(returnDate), loan.id());
            if (closed == 0) {
                return false;
            }
            update(c, "UPDATE recursos SET estado_recurso = 'Disponible' "
                    + "WHERE id_recurso = ? AND estado_recurso = 'Prestado'", loan.resourceId());
            return true;
        });
    }

    @Override
    public int markOverdue(LocalDate today) {
        return inTransaction(c -> {
            int total = 0;
            for (BorrowerType type : BorrowerType.values()) {
                total += update(c, "UPDATE " + loanTable(type) + " SET estado_prestamo = 'Vencido' "
                        + "WHERE estado_prestamo = 'Activo' AND fecha_devolucion_esperada < ?", toSql(today));
            }
            return total;
        });
    }

    @Override
    public List<Loan> findOpenLoans() {
        return queryLoans("p.estado_prestamo IN " + OPEN_STATES);
    }

    @Override
    public List<Loan> findOverdue(LocalDate today) {
        return queryLoans("p.estado_prestamo IN " + OPEN_STATES + " AND p.fecha_devolucion_esperada < ?",
                toSql(today));
    }

    @Override
    public Optional<Loan> findOpenLoanByResource(int resourceId) {
        List<Loan> loans = queryLoans("p.estado_prestamo IN " + OPEN_STATES + " AND p.id_recurso = ?", resourceId);
        return loans.isEmpty() ? Optional.empty() : Optional.of(loans.get(0));
    }

    /** Une los préstamos de estudiantes y de profesores; {@code where} usa el alias {@code p} y a lo sumo un parámetro. */
    private List<Loan> queryLoans(String where, Object... params) {
        String sql = loanSelect(BorrowerType.STUDENT, where) + " UNION ALL " + loanSelect(BorrowerType.TEACHER, where)
                + " ORDER BY fecha_devolucion_esperada, codigo_recurso";
        List<Object> all = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            for (Object param : params) {
                all.add(param);
            }
        }
        return query(sql, this::mapLoan, all.toArray());
    }

    private String loanSelect(BorrowerType type, String where) {
        boolean student = type == BorrowerType.STUDENT;
        String person = student ? "estudiantes" : "profesores";
        return "SELECT '" + type.label() + "' AS tipo, p.id_prestamo, p.id_recurso, r.codigo_recurso, "
                + "r.nombre_recurso, p." + borrowerColumn(type) + " AS id_prestatario, "
                + "CONCAT_WS(' ', b.primer_nombre, b.segundo_nombre, b.primer_apellido, b.segundo_apellido) AS nombre, "
                + "p.fecha_prestamo, p.fecha_devolucion_esperada, p.fecha_devolucion_real, p.estado_prestamo "
                + "FROM " + loanTable(type) + " p JOIN recursos r ON r.id_recurso = p.id_recurso "
                + "JOIN " + person + " b ON b." + borrowerColumn(type) + " = p." + borrowerColumn(type)
                + " WHERE " + where;
    }

    private static String loanTable(BorrowerType type) {
        return type == BorrowerType.STUDENT ? "prestamos_estudiante" : "prestamos_profesor";
    }

    private static String borrowerColumn(BorrowerType type) {
        return type == BorrowerType.STUDENT ? "id_estudiante" : "id_profesor";
    }

    private static Date toSql(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private static LocalDate toLocal(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private ResourceCategory mapCategory(ResultSet rs) throws SQLException {
        return new ResourceCategory(rs.getInt("id_categoria"), rs.getString("nombre_categoria"),
                rs.getString("descripcion"));
    }

    private Resource mapResource(ResultSet rs) throws SQLException {
        return new Resource(rs.getInt("id_recurso"), rs.getString("codigo_recurso"),
                rs.getString("nombre_recurso"), rs.getInt("id_categoria"), rs.getString("nombre_categoria"),
                rs.getString("descripcion"), toLocal(rs.getDate("fecha_adquisicion")),
                rs.getString("ubicacion_fisica"), ResourceStatus.fromLabel(rs.getString("estado_recurso")));
    }

    private Loan mapLoan(ResultSet rs) throws SQLException {
        BorrowerType type = "Estudiante".equals(rs.getString("tipo")) ? BorrowerType.STUDENT : BorrowerType.TEACHER;
        return new Loan(rs.getInt("id_prestamo"), rs.getInt("id_recurso"), rs.getString("codigo_recurso"),
                rs.getString("nombre_recurso"), type, rs.getInt("id_prestatario"), rs.getString("nombre"),
                toLocal(rs.getDate("fecha_prestamo")), toLocal(rs.getDate("fecha_devolucion_esperada")),
                toLocal(rs.getDate("fecha_devolucion_real")), rs.getString("estado_prestamo"));
    }
}
