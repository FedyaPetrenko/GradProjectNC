package com.grad.project.nc.persistence.impl;

import com.grad.project.nc.model.*;
import com.grad.project.nc.model.proxy.ProductProxy;
import com.grad.project.nc.persistence.*;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;

/**
 * edited by Pavlo Rospopa
 */
@Repository
public class ProductDaoImpl extends AbstractDao<Product> implements ProductDao {

    private static final String PK_COLUMN_NAME = "product_id";

    private final ObjectFactory<ProductProxy> proxyFactory;

    @Autowired
    public ProductDaoImpl(JdbcTemplate jdbcTemplate, ObjectFactory<ProductProxy> proxyFactory) {
        super(jdbcTemplate);
        this.proxyFactory = proxyFactory;
    }

    @Override
    public Product add(Product product) {
        String insertQuery = "INSERT INTO \"product\" (\"product_name\", \"product_description\", \"is_active\", " +
                "\"product_type_id\") VALUES (?, ?, ?, ?)";

        Long productId = executeInsertWithId(insertQuery, PK_COLUMN_NAME, product.getName(), product.getDescription(),
                product.getIsActive(), product.getProductType().getProductTypeId());

        product.setProductId(productId);

        return product;
    }

    @Override
    public Product find(Long id) {
        String query = "SELECT \"product_id\", \"product_name\", \"product_description\", \"is_active\", " +
                "\"product_type_id\" FROM \"product\" WHERE \"product_id\"=?";
        return findOne(query, new ProductRowMapper(), id);
    }

    @Override
    public Product update(Product product) {
        String updateQuery = "UPDATE \"product\" SET " +
                "\"product_name\"=?, " +
                "\"product_description\" = ?, " +
                "\"is_active\"=?, " +
                "\"product_type_id\"=? " +
                "WHERE \"product_id\"=?";

        executeUpdate(updateQuery, product.getName(), product.getDescription(), product.getIsActive(),
                product.getProductType().getProductTypeId());

        return product;
    }

    @Override
    public void delete(Product product) {
        String deleteQuery = "DELETE FROM \"product\" WHERE \"product_id\" =?";
        executeUpdate(deleteQuery, product.getProductId());
    }

    @Override
    public List<Product> findAll() {
        String findAllQuery = "SELECT \"product_id\", \"product_name\", \"product_description\", \"is_active\", " +
                "\"product_type_id\" FROM \"product\"";

        return query(findAllQuery, new ProductRowMapper());
    }

    @Override
    public List<Product> findProductsByRegion(Region region) {
        String findProductByRegionQuery = "SELECT p.\"product_id\", p.\"product_name\", p.\"product_description\", " +
                "p.\"is_active\", p.\"product_type_id\" FROM \"product\" p " +
                "JOIN \"product_region_price\" prp " +
                "ON p.\"product_id\" = prp.\"product_id\" " +
                "WHERE prp.\"region_id\" =?";

        return query(findProductByRegionQuery, new ProductRowMapper(), region.getRegionId());
    }

    @Override
    public Product findByName(String productName) {
        String findByNameQuery = "SELECT \"product_id\", \"product_name\", \"product_description\", \"is_active\", " +
                "\"product_type_id\" FROM \"product\" WHERE \"product_name\"=?";

        return findOne(findByNameQuery, new ProductRowMapper(), productName);
    }

    @Override
    public List<Product> findProductsByType(ProductType productType) {
        String findByProductTypeQuery = "SELECT \"product_id\", \"product_name\", \"product_description\", \"is_active\", " +
                "\"product_type_id\" FROM \"product\" WHERE \"product_type_id\"=?";

        return query(findByProductTypeQuery, new ProductRowMapper(), productType.getProductTypeId());
    }

    @Override
    public Product getProductByProductRegionPrice(ProductRegionPrice productRegionPrice) {
        String query = "SELECT pr.\"product_id\", pr.\"product_name\", pr.\"product_description\", pr.\"is_active\", " +
                "pr.\"product_type_id\" FROM \"product\" pr " +
                "WHERE pr.\"product_id\" = " +
                "(SELECT prp.\"product_id\" " +
                "FROM \"product_region_price\" prp WHERE prp.\"price_id\" = ?)";

        return findOne(query, new ProductRowMapper(), productRegionPrice.getPriceId());
    }

    private class ProductRowMapper implements RowMapper<Product> {

        @Override
        public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
            ProductProxy product = proxyFactory.getObject();

            product.setProductId(rs.getLong("product_id"));
            product.setName(rs.getString("product_name"));
            product.setDescription(rs.getString("product_description"));
            product.setIsActive(rs.getBoolean("is_active"));
            product.setProductTypeId(rs.getLong("product_type_id"));

            return product;
        }
    }
}