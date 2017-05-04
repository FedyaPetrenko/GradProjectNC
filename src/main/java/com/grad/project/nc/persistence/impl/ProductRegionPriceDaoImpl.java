package com.grad.project.nc.persistence.impl;

import com.grad.project.nc.model.Discount;
import com.grad.project.nc.model.Product;
import com.grad.project.nc.model.ProductRegionPrice;
import com.grad.project.nc.model.proxy.ProductRegionPriceProxy;
import com.grad.project.nc.persistence.AbstractDao;
import com.grad.project.nc.persistence.ProductRegionPriceDao;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * created by DeniG on 5/01/2017. edited by Pavlo Rospopa
 */
@Repository
public class ProductRegionPriceDaoImpl extends AbstractDao<ProductRegionPrice> implements ProductRegionPriceDao {

    private static final String PK_COLUMN_NAME = "price_id";

    private final ObjectFactory<ProductRegionPriceProxy> proxyFactory;

    @Autowired
    public ProductRegionPriceDaoImpl(JdbcTemplate jdbcTemplate, ObjectFactory<ProductRegionPriceProxy> proxyFactory) {
        super(jdbcTemplate);
        this.proxyFactory = proxyFactory;
    }

    @Override
    public ProductRegionPrice add(ProductRegionPrice productRegionPrice) {
        String insertQuery = "INSERT INTO \"product_region_price\" (\"product_id\", \"region_id\", \"price\") " +
                "VALUES(?,?,?)";

        Long prpId = executeInsertWithId(insertQuery, PK_COLUMN_NAME, productRegionPrice.getProduct().getProductId(),
                productRegionPrice.getRegion().getRegionId(), productRegionPrice.getPrice());

        productRegionPrice.setPriceId(prpId);

        return productRegionPrice;
    }

    @Override
    @Transactional
    public ProductRegionPrice update(ProductRegionPrice productRegionPrice) {
        String updateQuery = "UPDATE \"product_region_price\" SET \"product_id\"=?, \"region_id\"=?, " +
                "\"price\"=? WHERE \"price_id\"=?";

        executeUpdate(updateQuery, productRegionPrice.getProduct().getProductId(),
                productRegionPrice.getRegion().getRegionId(), productRegionPrice.getPrice(),
                productRegionPrice.getPriceId());

        return productRegionPrice;
    }

    @Override
    public ProductRegionPrice find(Long id) {
        String query = "SELECT \"price_id\", \"product_id\", \"region_id\", \"price\" " +
                "FROM \"product_region_price\" WHERE \"price_id\"=?";

        return findOne(query, new ProductRegionPriceRowMapper(), id);
    }

    @Override
    public List<ProductRegionPrice> findAll() {
        String findAllQuery = "SELECT \"price_id\", \"product_id\", \"region_id\", \"price\" " +
                "FROM \"product_region_price\"";

        return query(findAllQuery, new ProductRegionPriceRowMapper());
    }

    @Override
    public void delete(ProductRegionPrice productRegionPrice) {
        String deleteQuery = "DELETE FROM \"product_region_price\" WHERE \"price_id\"=?";
        executeUpdate(deleteQuery, productRegionPrice.getPriceId());
    }

    @Override
    public List<ProductRegionPrice> getProductRegionPricesByDiscount(Discount discount) {
        String query = "SELECT prp.\"price_id\", prp.\"product_id\", prp.\"region_id\", prp.\"price\" " +
                "FROM \"product_region_price\" prp " +
                "INNER JOIN \"discount_price\" dp " +
                "ON prp.\"price_id\"=dp.\"price_id\" " +
                "WHERE dp.\"discount_id\" = ?";


        return query(query, new ProductRegionPriceRowMapper(), discount.getDiscountId());
    }

    @Override
    public void deleteAllDiscounts(ProductRegionPrice productRegionPrice) {
        String query = "DELETE FROM \"discount_price\" WHERE \"price_id\" = ?";

        executeUpdate(query, productRegionPrice.getPriceId());
    }

    @Override
    public void saveAllDiscounts(ProductRegionPrice productRegionPrice) {
        deleteAllDiscounts(productRegionPrice);

        String insertQuery = "INSERT INTO \"discount_price\" (\"discount_id\", \"price_id\") VALUES (?, ?)";
        List<Object[]> batchArgs = new ArrayList<>();
        for (Discount discount : productRegionPrice.getDiscounts()) {
            batchArgs.add(new Object[]{discount.getDiscountId(), productRegionPrice.getPriceId()});
        }
        batchUpdate(insertQuery, batchArgs);
    }

    @Override
    public void addBatch(List<ProductRegionPrice> prices) {
        String insertQuery = "INSERT INTO \"product_region_price\" (\"product_id\", \"region_id\", \"price\") " +
                "VALUES(?,?,?)";

        List<Object[]> batchArgs = new ArrayList<>();
        for (ProductRegionPrice price : prices) {
            batchArgs.add(new Object[]{price.getProduct().getProductId(),
                    price.getRegion().getRegionId(), price.getPrice()});
        }
        batchUpdate(insertQuery, batchArgs);
    }

    @Override
    public List<ProductRegionPrice> getPricesByProductId(Long productId) {
        String query = "SELECT prp.\"price_id\", prp.\"product_id\", prp.\"region_id\", prp.\"price\" " +
                "FROM \"product_region_price\" prp " +
                "WHERE prp.\"product_id\"=?";

        return query(query, new ProductRegionPriceRowMapper(), productId);
    }

    private final class ProductRegionPriceRowMapper implements RowMapper<ProductRegionPrice> {
        @Override
        public ProductRegionPrice mapRow(ResultSet rs, int rowNum) throws SQLException {
            ProductRegionPriceProxy productRegionPrice = proxyFactory.getObject();

            productRegionPrice.setPriceId(rs.getLong("price_id"));
            productRegionPrice.setProductId(rs.getLong("product_id"));
            productRegionPrice.setRegionId(rs.getLong("region_id"));
            productRegionPrice.setPrice(rs.getDouble("price"));

            return productRegionPrice;
        }
    }
}