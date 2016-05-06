package com.excilys.computer_database.database.services;

import java.util.LinkedList;
import java.util.List;

import com.excilys.computer_database.database.DBManager;
import com.excilys.computer_database.database.dao.CompanyDAO;
import com.excilys.computer_database.database.dao.ComputerDAO;
import com.excilys.computer_database.database.dao.DAOException;
import com.excilys.computer_database.database.dtos.CompanyDTO;
import com.excilys.computer_database.entity.Company;
import com.excilys.computer_database.ui.Page;

public class CompaniesService {
    private CompanyDAO dao;

    /** Constructor by default. */
    public CompaniesService() {
        dao = CompanyDAO.getInstance();
    }

    /**
     * @return A page containing a number of "nbPerPage" companies from the
     *         position "begining" (when ranked by if).
     * @param begining The begining of the listed companies in the DB
     * @param nbPerPage Number of Companies to fetch
     * @throws DAOException
     */
    public Page<Company> listSomeCompanies(int begining, int nbPerPage) throws DAOException {
        int pageNumber = begining / nbPerPage + 1;
        List<Company> list = dao.findSome(begining, nbPerPage, CompanyDAO.NAME);
        DBManager.closeConnection();

        return new Page<Company>(list, pageNumber, nbPerPage);
    }

    /**
     * Return the lis of all the companies.
     * @return The list of all listed companies
     * @throws DAOException
     */
    public List<Company> listAllCompanies() throws DAOException {
        List<Company> list = dao.findAll();
        DBManager.closeConnection();
        return list;
    }

    /**
     * Delete the company of id "id", and the associated computers via a
     * transaction.
     * @param id The computer's id to delete
     * @throws DAOException
     */
    public void delete(Long id) throws DAOException {
        // Use here a transaction, to delete related computers
        DBManager.initTransaction();

        try {
            // Delete related computers
            (ComputerDAO.getInstance()).deleteByCompanyID(id);

            // Delete the company
            dao.delete(id);

            // Terminate the transaction
            DBManager.commit();
        } catch (DAOException e) {
            // Rollback if a problem is detected
            DBManager.rollback();
        }

        DBManager.closeConnection();
    }

    public Company find(Long id) throws DAOException {
        Company company = dao.find(id);
        DBManager.closeConnection();
        return company;
    }

    /**
     * Return the list of all companies (DTO form)
     * @return Return the list of all companies (DTO form)
     * @throws DAOException If an error occurs in the DAO
     */
    public List<CompanyDTO> getDTOList() throws DAOException {
        // TODO : Améliorer la complexité de l'obtention des DTO (complexité 2*n
        // pour le moment)
        List<Company> list = listAllCompanies();
        List<CompanyDTO> dtoList = new LinkedList<CompanyDTO>();
        for (Company company : list) {
            dtoList.add(new CompanyDTO(company));
        }

        DBManager.closeConnection();

        return dtoList;
    }
}