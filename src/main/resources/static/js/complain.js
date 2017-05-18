/**
 * Created by DeniG on 16.05.2017.
 */

var selectedComplain = -1;
var complaintListSize = 10;
var complaintListCurrentPage = 0;
var complaintsData;
var modalAlert;
var selectUserId;
var currentUserId;

function loadComplaints() {
    $.ajax({
        url: "/api/pmg/complaint/get/all/size/" + (complaintListSize + 1) + "/offset/" + complaintListSize * complaintListCurrentPage,
        success: function (data) {
            $("#no-complain-selected-alert").removeAttr("hidden");
            list = $("#complain-list");
            list.empty();
            complaintsData = data;
            data.forEach(function (item, i) {
                if (i < complaintListSize) {
                    ref = document.createElement("a");
                    complaintName = item.complainReason + "_" + item.productInstanceName;
                    ref.appendChild(document.createTextNode(complaintName));
                    ref.className = "list-group-item";
                    ref.href = "#";
                    ref.onclick = function () {
                        selectComplaint(i);
                    };
                    list.append(ref);
                }
            });
            prevPage = $("#complaint-btn-prev");
            nextPage = $("#complaint-btn-next");
            nextPage.attr("disabled","disabled");
            prevPage.attr("disabled","disabled");
            if (complaintListCurrentPage > 0) {
                prevPage.removeAttr("disabled");
            }
            if (data.length > complaintListSize) {
                nextPage.removeAttr("disabled");
            }
            selectComplaint(selectedComplain);
        },
        error: function () {
            console.error("Cannot load list complaints");
            alertError("Internal server error!")
        }
    });
}

function selectComplaint(index) {
    list = $("#complain-list");
    selectedComplain = index;
    if (index == -1) {
        $("#no-complain-selected-alert").removeClass("hidden");
        $("#complain-info-panel").addClass("hidden");
        list.find("a").removeClass("active");
    } else {
        $("#no-complain-selected-alert").addClass("hidden");
        $("#complain-info-panel").removeClass("hidden");
        list.find("a").removeClass("active");
        list.find("a:nth-child(" + (selectedComplain + 1) + ")").addClass("active");
    }
    if (selectedComplain != -1) {
        $("#complain-user-email").val(complaintsData[selectedComplain].userEmail);
        //TODO add other user data
        $("#complain-responsible-email").val(complaintsData[selectedComplain].responsibleEmail);
        //TODO add other responsible data
        $("#complain-instance-name").val(complaintsData[selectedComplain].productInstanceName);
        $("#selected-complain-stats").val(complaintsData[selectedComplain].status);
        $("#selected-complain-reason").val(complaintsData[selectedComplain].complainReason);

        $("#selected-complain-start-date").val(moment.unix(complaintsData[selectedComplain].openDate).format("LLL"));
        if (complaintsData[selectedComplain].closeDate != null) {
            $("#selected-complain-end-date").val(moment.unix(complaintsData[selectedComplain].closeDate).format("LLL"));
        }
        $("#selected-complain-title").val(complaintsData[selectedComplain].complainTitle);
        $("#selected-complain-content").val(complaintsData[selectedComplain].content);
        $("#selected-complain-responce").val(complaintsData[selectedComplain].response);
        if (complaintsData[selectedComplain].status == "CONSIDERATION_COMPLETED"
            || (complaintsData[selectedComplain].status == "UNDER_CONSIDERATION"
            && currentUserId != complaintsData[selectedComplain].responsiblId)) {
            $("#selected-complain-responce").attr("readonly", "readonly");
        } else {
            $("#selected-complain-responce").removeAttr("readonly", "readonly");
        }
        loadControlButtons();
    }
}

function loadDomainsInModal(keyCode) {
    if (keyCode != 13) {
        return;
    }
    modalAlert = $("#new-complaint-modal-error-msg");
    $("#new-complaint-domain").empty();
    if ($("#new-complaint-user-email").val().length < 1) {
        modalAlert.html("<strong>Warning! </strong> E-mail field is empty!");
        modalAlert.removeClass("hidden");
        clearNewComplaintModalFormByIncorrectEmail();
    } else {
        $.ajax({
            url: "/api/pmg/domains/find/bymail/" + $("#new-complaint-user-email").val() + "/",
            success: function (data) {
                if (data.status == "not found") {
                    modalAlert.html("<strong> Warning! </strong> Email not found");
                    modalAlert.removeClass("hidden");
                    clearNewComplaintModalFormByIncorrectEmail();
                } else {
                    selectUserId = data.userId;
                    if (data.domains.length > 0) {
                        options = $("#new-complaint-domain");
                        data.domains.forEach(function (item, i) {
                            modalAlert.empty();
                            modalAlert.addClass("hidden");
                            option = document.createElement("option");
                            if (i == 0) {
                                option.setAttribute("selected", "selected");
                            }
                            option.setAttribute("value", item.domainId);
                            option.appendChild(document.createTextNode(item.domainName));
                            options.append(option);
                        });
                        options.removeAttr("disabled");
                        loadProductInstancesInModal();
                    } else {
                        modalAlert.html("<strong> Warning! </strong> This user does not have any domains!");
                        modalAlert.removeClass("hidden");
                        clearNewComplaintModalFormByIncorrectEmail();
                    }
                }
            },
            error: function () {
                modalAlert.html("<strong>Error! </strong> Internal server error!");
                modalAlert.removeClass("hidden");
                clearNewComplaintModalFormByIncorrectEmail();
            }
        });
    }
}

function loadProductInstancesInModal() {
    modalAlert = $("#new-complaint-modal-error-msg");
    $.ajax({
        url: "/api/pmg/instances/find/bydomain/" + $("#new-complaint-domain").val() + "/",
        success: function (data) {
            clearNewComplaintModalFormByIncorrectDomain();
            if (data.length > 0) {
                modalAlert.empty();
                modalAlert.addClass("hidden");
                options = $("#new-complaint-instanse");
                data.forEach(function (item, i) {
                    var option = document.createElement("option");
                    option.setAttribute("value", item.instanceId);
                    if (i == 0) {
                        option.setAttribute("selected", "selected");
                    }
                    option.appendChild(document.createTextNode(item.product.productName));
                    options.append(option);
                });
                options.removeAttr("disabled");
                $("#new-complaint-subject").removeAttr("disabled");
                $("#new-complaint-reason").removeAttr("disabled");
                $("#new-complaint-content").removeAttr("readonly");
                $("#create-complaint-ftom-modal-btn").removeAttr("disabled");
            } else {
                clearNewComplaintModalFormByIncorrectDomain();
                modalAlert.html("<strong>Warning! </strong> This domain does not have any instances!");
                modalAlert.removeClass("hidden");
            }
        },
        error: function () {
            clearNewComplaintModalFormByIncorrectDomain();
            modalAlert.html("<strong>Error! </strong> Internal server error!");
            modalAlert.removeClass("hidden");
        }

    });
}

function clearNewComplaintModalForm() {
    $("#new-complaint-user-email").val("");
    clearNewComplaintModalFormByIncorrectEmail();
}
function clearNewComplaintModalFormByIncorrectEmail() {
    $("#new-complaint-domain").attr("disabled", "disabled");
    $("#new-complaint-domain").empty();
    clearNewComplaintModalFormByIncorrectDomain();
}
function clearNewComplaintModalFormByIncorrectDomain() {
    $("#new-complaint-instanse").attr("disabled", "disabled");
    $("#new-complaint-instanse").empty();
    $("#new-complaint-subject").attr("disabled", "disabled");
    $("#new-complaint-subject").val("");
    $("#new-complaint-content").attr("readonly", "readonly");
    $("#new-complaint-content").val("");
    $("#new-complaint-reason").attr("disabled", "disabled");
    $("#create-complaint-ftom-modal-btn").attr("disabled", "disabled");
}

function openNewComplaintModal() {
    modalAlert = $("#new-complaint-modal-error-msg")
    modalAlert.empty();
    modalAlert.addClass("hidden");
    clearNewComplaintModalForm();

    reasons = $("#new-complaint-reason");
    reasons.attr("disabled", "disabled");
    reasons.empty();
    $.ajax({
        url: "/api/pmg/category/get/bytype/COMPLAIN_REASON/",
        success: function (data) {
            if (data.status = "found") {
                data.categories.forEach(function (item, i) {
                    option = document.createElement("option");
                    if (i == 0) {
                        option.setAttribute("selected", "selected");
                    }
                    option.setAttribute("value", item.categoryId);
                    option.appendChild(document.createTextNode(item.categoryName));
                    reasons.append(option);
                });
            }
            else {
                modalAlert.html("<strong>Error! </strong> Complaint`s reasons not found");
                modalAlert.removeClass("hidden");
                clearNewComplaintModalForm();
            }
        },
        error: function () {
            modalAlert.html("<strong>Error! </strong> Internal server error!");
            modalAlert.removeClass("hidden");
            clearNewComplaintModalForm();
        }
    });
}
function createNewComplaintFromModal() {
    $.ajax({
        url: "/api/pmg/complaint/new/",
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': $('meta[name=_csrf]').attr("content")
        },
        contentType: 'application/json',
        data: JSON.stringify({
            userId: selectUserId,
            instanceId: $("#new-complaint-instanse").val(),
            reasonId: $("#new-complaint-reason").val(),
            title: $("#new-complaint-subject").val(),
            content: $("#new-complaint-content").val()
        }),
        success: function (data) {
            loadComplaints();
            printResult(data);
        },
        error: function () {
            alertError("Internal server error!")
        }
    });
}

function getPrevPage() {
    if (complaintListCurrentPage > 0) {
        complaintListCurrentPage -= 1;
    }
    selectedComplain = -1;
    loadComplaints();
}

function getNextPage() {
    complaintListCurrentPage++;
    selectedComplain = -1;
    loadComplaints();
}

function takeComplaintForConsideration() {
    $.ajax({
        url: "/api/pmg/complaint/take/byId/",
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': $('meta[name=_csrf]').attr("content")
        },
        contentType: 'application/json',
        data: JSON.stringify({
            userId: currentUserId,
            complaintId: complaintsData[selectedComplain].complainId
        }),
        success: function (data) {
            loadComplaints();
            printResult(data);
        },
        error: function () {
            alertError("Internal server error!")
        }
    });
}
function updateComplaint() {
    $.ajax({
        url: "/api/pmg/complaint/update/response/",
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': $('meta[name=_csrf]').attr("content")
        },
        contentType: 'application/json',
        data: JSON.stringify({
            userId: currentUserId,
            complaintId: complaintsData[selectedComplain].complainId,
            response: $("#selected-complain-responce").val()
        }),
        success: function (data) {
            loadComplaints();
            printResult(data);
        },
        error: function () {
            alertError("Internal server error!")
        }
    });
}

function completComplaintConsideration() {
    $.ajax({
        url: "/api/pmg/complaint/complete/byid/",
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': $('meta[name=_csrf]').attr("content")
        },
        contentType: 'application/json',
        data: JSON.stringify({
            userId: currentUserId,
            complaintId: complaintsData[selectedComplain].complainId
        }),
        success: function (data) {
            selectedComplain = 0;
            loadComplaints();
            printResult(data);
        },
        error: function () {
            alertError("Internal server error!")
        }
    });
}

function loadControlButtons() {
    takeComplaintBtn = $("#take-complaint-btn");
    updateComplaintBtn = $("#update-complaint-btn");
    completComplaintBtn = $("#complet-consideration-complaint-btn");
    takeComplaintBtn.attr("disabled", "disabled");
    takeComplaintBtn.addClass("hidden");
    updateComplaintBtn.attr("disabled", "disabled");
    updateComplaintBtn.addClass("hidden");
    completComplaintBtn.attr("disabled", "disabled");
    completComplaintBtn.addClass("hidden");
    addButtons();


}

function addButtons() {

    switch (complaintsData[selectedComplain].status) {
        case "CREATED":
            takeComplaintBtn.removeAttr("disabled");
            takeComplaintBtn.removeClass("hidden");
            updateComplaintBtn.removeAttr("disabled");
            updateComplaintBtn.removeClass("hidden");
            break;

        case "UNDER_CONSIDERATION":
            if (currentUserId == complaintsData[selectedComplain].responsiblId) {
                completComplaintBtn.removeAttr("disabled");
                completComplaintBtn.removeClass("hidden");
                updateComplaintBtn.removeAttr("disabled");
                updateComplaintBtn.removeClass("hidden");
            }
            break;

        case "CONSIDERATION_COMPLETED":

            break;
    }
}

function alertError(msg) {
    container = $("#alerts-bar");
    alert = document.createElement("div");
    alert.className = "alert alert-danger alert-dismissable";
    alert.appendChild(document.createTextNode(msg));
    ref = document.createElement("a");
    ref.appendChild(document.createTextNode("X"));
    ref.href = "#";
    ref.className = "close"
    ref.setAttribute("data-dismiss", "alert");
    ref.setAttribute("aria-label", "close");
    alert.appendChild(ref);
    container.append(alert);
}

function alertSuccess(msg) {
    container = $("#alerts-bar");
    alert = document.createElement("div");
    alert.className = "alert alert-success alert-dismissable";
    alert.appendChild(document.createTextNode(msg));
    ref = document.createElement("a");
    ref.appendChild(document.createTextNode("X"));
    ref.href = "#";
    ref.className = "close"
    ref.setAttribute("data-dismiss", "alert");
    ref.setAttribute("aria-label", "close");
    alert.appendChild(ref);
    container.append(alert);
}

function printResult(data) {
    if(data.status == "success"){
        alertSuccess(data.message)
    } else {
        alertError(data.message)
    }
}

$(document).ready(function () {
    $.ajax({
        url: "/api/user/account",
        success: function (data) {
            currentUserId = data.userId;
            loadComplaints();
        },
        error: function () {
            alertError("Internal server error!")

        }
    });


});