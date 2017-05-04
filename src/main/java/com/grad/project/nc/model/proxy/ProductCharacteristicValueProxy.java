package com.grad.project.nc.model.proxy;

import com.grad.project.nc.model.Product;
import com.grad.project.nc.model.ProductCharacteristic;
import com.grad.project.nc.model.ProductCharacteristicValue;
import com.grad.project.nc.persistence.ProductCharacteristicDao;
import com.grad.project.nc.persistence.ProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ProductCharacteristicValueProxy extends ProductCharacteristicValue {

    private ProductDao productDao;
    private ProductCharacteristicDao productCharacteristicDao;

    private Long productId;
    private Long productCharacteristicId;

    @Autowired
    public ProductCharacteristicValueProxy(ProductCharacteristicDao productCharacteristicDao, ProductDao productDao) {
        this.productCharacteristicDao = productCharacteristicDao;
        this.productDao = productDao;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getProductCharacteristicId() {
        return productCharacteristicId;
    }

    public void setProductCharacteristicId(Long productCharacteristicId) {
        this.productCharacteristicId = productCharacteristicId;
    }

    @Override
    public Product getProduct() {
        if (super.getProduct() == null) {
            super.setProduct(productDao.find(getProductId()));
        }
        return super.getProduct();
    }

    @Override
    public ProductCharacteristic getProductCharacteristic() {
        if (super.getProductCharacteristic() == null) {
            super.setProductCharacteristic(productCharacteristicDao.find(getProductCharacteristicId()));
        }
        return super.getProductCharacteristic();
    }
}