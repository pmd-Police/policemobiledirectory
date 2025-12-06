package com.example.policemobiledirectory.data.mapper

import com.example.policemobiledirectory.data.local.EmployeeEntity
import com.example.policemobiledirectory.model.Employee

fun EmployeeEntity.toEmployee(): Employee = Employee(
    kgid = kgid,
    name = name,
    email = email,
    pin = pin,
    mobile1 = mobile1,
    mobile2 = mobile2,
    rank = rank,
    metalNumber = metalNumber,
    district = district,
    station = station,
    bloodGroup = bloodGroup,
    photoUrl = photoUrl,
    photoUrlFromGoogle = photoUrlFromGoogle,
    fcmToken = fcmToken,
    firebaseUid = firebaseUid,
    isAdmin = isAdmin,
    isApproved = isApproved,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Employee.toEntity(): EmployeeEntity = EmployeeEntity(
    kgid = kgid,
    name = name,
    email = email,
    pin = pin,
    mobile1 = mobile1,
    mobile2 = mobile2,
    rank = rank,
    metalNumber = metalNumber,
    district = district,
    station = station,
    bloodGroup = bloodGroup,
    photoUrl = photoUrl,
    photoUrlFromGoogle = photoUrlFromGoogle,
    fcmToken = fcmToken,
    firebaseUid = firebaseUid,
    isAdmin = isAdmin,
    isApproved = isApproved,
    createdAt = createdAt,
    updatedAt = updatedAt
)
