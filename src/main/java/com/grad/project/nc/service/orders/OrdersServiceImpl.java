package com.grad.project.nc.service.orders;

import com.grad.project.nc.service.exceptions.ServiceSecurityException;
import com.grad.project.nc.model.*;
import com.grad.project.nc.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

//TODO security

@Service
public class OrdersServiceImpl implements OrdersService {

    private UserDao userDao;
    private ProductOrderDao orderDao;
    private ProductRegionPriceDao productRegionPriceDao;
    private DomainDao domainDao;
    private ProductInstanceDao productInstanceDao;
    private CategoryDao categoryDao;
    private ProductDao productDao;

    private static final long INSTANCE_STATUS_CREATED = 9L;
    private static final long INSTANCE_STATUS_ACTIVATED = 10L;
    private static final long INSTANCE_STATUS_SUSPENDED = 11L;
    private static final long INSTANCE_STATUS_DEACTIVATED = 12L;

    private static final long ORDER_AIM_CREATE = 13L;
    private static final long ORDER_AIM_SUSPEND = 14L;
    private static final long ORDER_AIM_DEACTIVATE = 15L;
    private static final long ORDER_AIM_RESUME = 25L;
    private static final long ORDER_AIM_MODIFY = 26L;

    private static final long ORDER_STATUS_CREATED = 1L;
    private static final long ORDER_STATUS_IN_PROGRESS = 2L;
    private static final long ORDER_STATUS_CANCELLED = 3L;
    private static final long ORDER_STATUS_COMPLETED = 4L;

    @Autowired
    public OrdersServiceImpl(UserDao userDao, ProductOrderDao orderDao, ProductRegionPriceDao productRegionPriceDao,
                             DomainDao domainDao, ProductInstanceDao productInstanceDao, CategoryDao categoryDao,
                             ProductDao productDao) {
        this.userDao = userDao;
        this.orderDao = orderDao;
        this.productRegionPriceDao = productRegionPriceDao;
        this.domainDao = domainDao;
        this.productInstanceDao = productInstanceDao;
        this.categoryDao = categoryDao;
        this.productDao = productDao;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof AnonymousAuthenticationToken)) {
            String username = ((UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
            return userDao.findByEmail(username).orElseThrow((Supplier<RuntimeException>) () -> new ServiceSecurityException("User must be authorised to use this"));
        } else {
            throw new ServiceSecurityException("User must be authorised to use this");
        }
    }

    private ProductOrder newOrder(long instanceId, long userId, long aimId) {
        ProductOrder order = new ProductOrder();

        order.setOpenDate(OffsetDateTime.now());
        order.setOrderAim(categoryDao.find(aimId));
        order.setProductInstance(productInstanceDao.find(instanceId));
        order.setUser(userDao.find(userId));
        order.setStatus(categoryDao.find(ORDER_STATUS_CREATED));

        return orderDao.add(order);
    }

    @Override
    public ProductOrder newCreationOrder(long productId, long domainId, long userId) {
        Domain domain = domainDao.find(domainId);

        ProductInstance instance = new ProductInstance();

        Category category = categoryDao.find(INSTANCE_STATUS_CREATED);

        instance.setDomain(domain);
        instance.setStatus(category);
        instance.setPrice(productRegionPriceDao.findByRegionIdAndProductId(
                domain.getAddress().getLocation().getRegion().getRegionId(), productId));

        instance = productInstanceDao.add(instance);

        return newOrder(instance.getInstanceId(), userId, ORDER_AIM_CREATE);
    }

    @Override
    public ProductOrder newSuspensionOrder(long instanceId, long userId) {
        return newOrder(instanceId, userId, ORDER_AIM_SUSPEND);
    }

    @Override
    public ProductOrder newContinueOrder(long instanceId, long userId) {
        return newOrder(instanceId, userId, ORDER_AIM_RESUME);
    }

    @Override
    public ProductOrder newDeactivationOrder(long instanceId, long userId) {
        return newOrder(instanceId, userId, ORDER_AIM_DEACTIVATE);
    }

    @Override
    public ProductOrder newCreationOrder(long productId, long domainId) {
        return newCreationOrder(productId, domainId, getCurrentUser().getUserId());
    }

    @Override
    public ProductOrder newSuspensionOrder(long instanceId) {
        return newSuspensionOrder(instanceId, getCurrentUser().getUserId());

    }

    @Override
    public ProductOrder newDeactivationOrder(long instanceId) {
        return newDeactivationOrder(instanceId, getCurrentUser().getUserId());
    }

    @Override
    public ProductOrder newContinueOrder(long instanceId) {
        return newContinueOrder(instanceId, getCurrentUser().getUserId());
    }

    @Override
    public void startOrder(long orderId) {
        ProductOrder order = orderDao.find(orderId);

        order.setStatus(categoryDao.find(ORDER_STATUS_IN_PROGRESS));
        order.setResponsible(getCurrentUser());
        orderDao.update(order);
    }

    @Override
    public void cancelOrder(long orderId) {
        ProductOrder order = orderDao.find(orderId);

        order.setStatus(categoryDao.find(ORDER_STATUS_CANCELLED));
        order.setCloseDate(OffsetDateTime.now());

        if (order.getOrderAim().getCategoryId() == ORDER_AIM_CREATE) {
            order.getProductInstance().setStatus(categoryDao.find(INSTANCE_STATUS_DEACTIVATED));
            productInstanceDao.update(order.getProductInstance());
//            productInstanceDao.delete(order.getProductInstance());
        }
        orderDao.update(order);
    }

    @Override
    public void completeOrder(long orderId) {
        ProductOrder order = orderDao.find(orderId);

        order.setStatus(categoryDao.find(ORDER_STATUS_COMPLETED));
        order.setCloseDate(OffsetDateTime.now());

        if (order.getOrderAim().getCategoryId() == ORDER_AIM_CREATE) {
            order.getProductInstance().setStatus(categoryDao.find(INSTANCE_STATUS_ACTIVATED));
        } else if (order.getOrderAim().getCategoryId() == ORDER_AIM_RESUME) {
            order.getProductInstance().setStatus(categoryDao.find(INSTANCE_STATUS_ACTIVATED));
        } else if (order.getOrderAim().getCategoryId() == ORDER_AIM_DEACTIVATE) {
            order.getProductInstance().setStatus(categoryDao.find(INSTANCE_STATUS_DEACTIVATED));
        } else if (order.getOrderAim().getCategoryId() == ORDER_AIM_SUSPEND) {
            order.getProductInstance().setStatus(categoryDao.find(INSTANCE_STATUS_SUSPENDED));
        }

        productInstanceDao.update(order.getProductInstance());
        orderDao.update(order);
    }

    @Override
    public Collection<ProductOrder> getUserOrders(long size, long offset) {
        return orderDao.findByUserId(getCurrentUser().getUserId());
    }

    @Override
    public Collection<ProductOrder> getAllOrders(long size, long offset) {
        List<ProductOrder> orders = orderDao.findAll();
        Collections.reverse(orders);
        return orders;
    }

    @Override
    public void userCancelOrder(Long id) {
        cancelOrder(id);
    }

    @Override
    public Collection<ProductOrder> getOrdersByProductInstance(Long id, Long size, Long offset) {
        List<ProductOrder> orders = new LinkedList<>(orderDao.findByProductInstanceId(id));
        Collections.reverse(orders);
        return orders;
    }

    @Override
    public ProductOrder updateOrderInfo(long orderId, long domainId, long productId) {
        ProductOrder order = orderDao.find(orderId);
        Domain domain = domainDao.find(domainId);

        ProductRegionPrice price = productRegionPriceDao.findByRegionIdAndProductId(
                domain.getAddress().getLocation().getRegion().getRegionId(), productId);

        if (price == null) {
            return null;
        } else {
            order.getProductInstance().setDomain(domain);
            order.getProductInstance().setPrice(price);
            productInstanceDao.update(order.getProductInstance());
            orderDao.update(order);
            return order;
        }
    }
}
