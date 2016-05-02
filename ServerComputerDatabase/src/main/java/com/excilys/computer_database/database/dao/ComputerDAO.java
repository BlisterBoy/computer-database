package com.excilys.computer_database.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.computer_database.database.ConnectionDB;
import com.excilys.computer_database.database.dtos.ComputerDTO;
import com.excilys.computer_database.database.dtos.ComputerDTOMapper;
import com.excilys.computer_database.database.mappers.Mapper;
import com.excilys.computer_database.entity.Company;
import com.excilys.computer_database.entity.Computer;
import com.excilys.computer_database.helpers.DateHelper;
import com.excilys.computer_database.ui.Page;

public class ComputerDAO extends DAO<Computer> implements Mapper<Computer, ResultSet> {
    // TODO : study the utility of the "volatile"
    private static volatile ComputerDAO instance = null;

    public static final String TABLE_NAME = "computer";
    private static final String ID = TABLE_NAME + ".id", NAME = TABLE_NAME + ".name",
            COMPANY_ID = TABLE_NAME + ".company_id", INTRODUCED = TABLE_NAME + ".introduced",
            DISCONTINUED = TABLE_NAME + ".discontinued";

    private static final String FIND_REQUEST = "SELECT * FROM " + TABLE_NAME + " LEFT JOIN " + CompanyDAO.TABLE_NAME
            + " ON " + COMPANY_ID + " = " + CompanyDAO.ID + " WHERE " + ID + " = ?",
            FIND_ALL_REQUEST = "SELECT * FROM " + TABLE_NAME + " LEFT JOIN " + CompanyDAO.TABLE_NAME + " ON "
                    + COMPANY_ID + " = " + CompanyDAO.ID,
                    INSERT_FULL_REQUEST = "INSERT INTO " + TABLE_NAME + " ( " + NAME + "," + INTRODUCED + "," + DISCONTINUED
                    + "," + COMPANY_ID + " ) VALUES (?,?,?,?) ",
                    UPDATE_REQUEST = "UPDATE " + TABLE_NAME + " SET " + NAME + " = ? , " + INTRODUCED + " = ? , " + DISCONTINUED
                    + " = ? , " + COMPANY_ID + " = ? WHERE " + ID + " = ?",
                    DELETE_REQUEST = "DELETE FROM " + TABLE_NAME + " WHERE " + ID + " = ? ",
                    COUNT_REQUEST = "SELECT COUNT(" + ID + ") FROM " + TABLE_NAME,
                    FIND_BY_NAME = "SELECT * FROM " + TABLE_NAME + " LEFT JOIN " + CompanyDAO.TABLE_NAME + " ON " + COMPANY_ID
                    + " = " + CompanyDAO.ID + " WHERE " + NAME + " LIKE ?  OR " + CompanyDAO.NAME
                    + " LIKE ? LIMIT ? OFFSET ?",
                    COUNT_FIND_BY_NAME = "SELECT COUNT(" + ID + ") FROM " + TABLE_NAME + " LEFT JOIN " + CompanyDAO.TABLE_NAME
                    + " ON " + COMPANY_ID + " = " + CompanyDAO.ID + " WHERE " + NAME + " LIKE ?  OR " + CompanyDAO.NAME
                    + " LIKE ?",
                    DELETE_LIST = "DELETE FROM " + TABLE_NAME + " WHERE " + ID + " IN ";

    private Logger logger;

    /** Constructor. */
    private ComputerDAO() {
        super();
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public String getFindRequest() {
        return FIND_REQUEST;
    }

    @Override
    public String getFindAllRequest() {
        return FIND_ALL_REQUEST;
    }

    @Override
    public Computer unmap(ResultSet rs) throws DAOException {
        // Extract the company
        Company company = (CompanyDAO.getInstance()).unmap(rs);

        // Build the Computer
        try {
            return new Computer.ComputerBuilder(rs.getString(NAME)).id(rs.getLong(ID))
                    .introduced(rs.getTimestamp(INTRODUCED)).discontinued(rs.getTimestamp(DISCONTINUED))
                    .company(company).build();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            throw new DAOException(e);
        }
    }

    /**
     * Factory pattern.
     *
     * @return The unique instance of ComputerDAO
     */
    public static final ComputerDAO getInstance() {
        if (ComputerDAO.instance == null) {
            synchronized (ComputerDAO.class) {
                if (ComputerDAO.instance == null) {
                    ComputerDAO.instance = new ComputerDAO();
                }
            }
        }

        return ComputerDAO.instance;
    }

    @Override
    public Computer create(Computer comp) throws DAOException {
        // comp must not have an id
        if (comp.getId() != null) {
            throw new DAOException("The object must have no id at the creation.");
        }

        // Exécution de la requête
        try (Connection con = ConnectionDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(INSERT_FULL_REQUEST, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, comp.getName());
                if (comp.getIntroduced() != null) {
                    stmt.setTimestamp(2, DateHelper.localDateToTimestamp(comp.getIntroduced()));
                } else {
                    stmt.setTimestamp(2, null);
                }
                if (comp.getDiscontinued() != null) {
                    stmt.setTimestamp(3, DateHelper.localDateToTimestamp(comp.getDiscontinued()));
                } else {
                    stmt.setTimestamp(3, null);
                }

                if (comp.getCompany() != null) {
                    stmt.setLong(4, comp.getCompany().getId());
                } else {
                    stmt.setNull(4, java.sql.Types.BIGINT);
                }

                stmt.executeUpdate();

                // Mise à jour de l'id de l'objet inséré
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.first()) {
                    comp.setId(rs.getLong(1));
                } else {
                    String errorMessage = "L'insertion n'a pas aboutie";
                    logger.error(errorMessage);
                    throw new SQLException(errorMessage);
                }

                return comp;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            throw new DAOException(e);
        }
    }

    @Override
    public Computer update(Computer comp) throws DAOException {
        try (Connection con = ConnectionDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(UPDATE_REQUEST)) {
                stmt.setString(1, comp.getName());
                stmt.setTimestamp(2, DateHelper.localDateToTimestamp(comp.getIntroduced()));
                stmt.setTimestamp(3, DateHelper.localDateToTimestamp(comp.getDiscontinued()));
                stmt.setLong(4, comp.getCompany().getId());
                stmt.setLong(5, comp.getId());

                stmt.executeUpdate();

                return comp;
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            throw new DAOException(e);
        }
    }

    @Override
    public void delete(Long id) throws DAOException {
        // TODO : Vérifier que l'id existe
        try (Connection con = ConnectionDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(DELETE_REQUEST)) {
                stmt.setLong(1, id);

                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            throw new DAOException(e);
        }
    }

    public int getCount() throws DAOException {
        try (Connection con = ConnectionDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(COUNT_REQUEST)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.first()) {
                    return rs.getInt(1);
                }
                return -1;
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    public Page<ComputerDTO> searchByName(String name, int begining, int nbPerPage) throws DAOException {
        int pageNumber = begining / nbPerPage;

        try (Connection con = ConnectionDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(FIND_BY_NAME)) {
                stmt.setString(1, '%' + name + '%');
                stmt.setString(2, '%' + name + '%');
                stmt.setInt(3, nbPerPage);
                stmt.setInt(4, begining);

                // Récupération de la liste
                List<ComputerDTO> l = new LinkedList<>();
                ResultSet rs = stmt.executeQuery();
                ComputerDTOMapper mapper = new ComputerDTOMapper();
                while (rs.next()) {
                    l.add(mapper.unmap(this.unmap(rs)));
                }

                return new Page<ComputerDTO>(l, pageNumber, nbPerPage);
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    public int countSearchByNameNbResult(String name) throws DAOException {
        try (Connection con = ConnectionDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(COUNT_FIND_BY_NAME)) {
                stmt.setString(1, '%' + name + '%');
                stmt.setString(2, '%' + name + '%');

                ResultSet rs = stmt.executeQuery();
                if (rs.first()) {
                    return rs.getInt(1);
                }
                return -1;
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    public void deleteComputerList(Long[] t) throws DAOException {
        // setArray(..) is not supported with mysql, so we have to implement it...
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        for(int i = 0; i < t.length; i++){
            if(i > 0) {
                sb.append(",");
            }
            sb.append(t[i]);
        }
        sb.append(")");


        try (Connection con = ConnectionDB.getConnection()) {
            try (PreparedStatement stmt = con.prepareStatement(DELETE_LIST + sb.toString())) {
                System.out.println(stmt.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new DAOException(e);
        }
    }
}
