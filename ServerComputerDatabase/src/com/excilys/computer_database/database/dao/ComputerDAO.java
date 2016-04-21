package com.excilys.computer_database.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.excilys.computer_database.database.ConnectionDB;
import com.excilys.computer_database.entity.Company;
import com.excilys.computer_database.entity.Computer;

public class ComputerDAO extends DAO<Computer> {
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
			INSERT_REQUEST_WITH_NAME = "INSERT INTO " + TABLE_NAME + " ( " + NAME + " ) VALUES (?) ",
			UPDATE_REQUEST = "UPDATE " + TABLE_NAME + " SET " + NAME + " = ? , " + INTRODUCED + " = ? , " + DISCONTINUED
					+ " = ? , " + COMPANY_ID + " = ? WHERE " + ID + " = ?",
			DELETE_REQUEST = "DELETE FROM " + TABLE_NAME + " WHERE " + ID + " = ? ";

	private ComputerDAO() {
		super();
	}

	public final static ComputerDAO getInstance() {
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
	public Computer unmap(ResultSet rs) throws SQLException {
		// Extract the company
		return new Computer.ComputerBuilder(rs.getString(NAME))
		
		return new Computer(rs.getLong(ID), , rs.getTimestamp(INTRODUCED),
				rs.getTimestamp(DISCONTINUED), rs.getLong(COMPANY_ID));
	}

	@Override
	public ResultSet map(Computer comp) throws SQLException {
		// TODO
		return null;
	}

	@Override
	public Computer create(Computer comp) throws SQLException {
		// Exécution de la requête
		try (Connection con = ConnectionDB.getConnection()) {
			try (PreparedStatement stmt = con.prepareStatement(INSERT_FULL_REQUEST, Statement.RETURN_GENERATED_KEYS)) {
				stmt.setString(1, comp.getName());
				stmt.setTimestamp(2, comp.getIntroduced());
				stmt.setTimestamp(3, comp.getDiscontinued());
				stmt.setLong(4, comp.getCompany_id());

				stmt.executeUpdate();

				// Mise à jour de l'id de l'objet inséré
				ResultSet rs = stmt.getGeneratedKeys();
				if (rs.first()) {
					comp.setId(rs.getLong(1));
				} else {
					throw new SQLException("L'insertion n'a pas aboutie");
				}

				return comp;
			}
		}
	}

	@Override
	public Computer update(Computer comp) throws SQLException {
		try (Connection con = ConnectionDB.getConnection()) {
			try (PreparedStatement stmt = con.prepareStatement(UPDATE_REQUEST)) {
				stmt.setString(1, comp.getName());
				stmt.setTimestamp(2, comp.getIntroduced());
				stmt.setTimestamp(3, comp.getDiscontinued());
				stmt.setLong(4, comp.getCompany().getId());
				stmt.setLong(5, comp.getId());

				stmt.executeUpdate();
			}
		}

		return comp;
	}

	@Override
	public void delete(Long id) throws SQLException {
		// TODO : Vérifier que l'id existe
		try (Connection con = ConnectionDB.getConnection()) {
			try (PreparedStatement stmt = con.prepareStatement(DELETE_REQUEST)) {
				stmt.setLong(1, id);

				stmt.executeUpdate();
			}
		}
	}

	@Override
	public String getFindRequest() {
		return FIND_ALL_REQUEST;
	}

	@Override
	public String getFindAllRequest() {
		return FIND_REQUEST;
	}
}
