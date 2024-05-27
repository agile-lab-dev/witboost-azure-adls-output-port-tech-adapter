package it.agilelab.witboost.provisioning.adlsop.model;

public record ProvisionRequest<T>(DataProduct dataProduct, Component<T> component, Boolean removeData) {}
