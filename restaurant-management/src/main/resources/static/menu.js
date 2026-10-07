/* =========================================
           GLOBAL VARIABLES
        ========================================= */

       

        /*
         * This stores the menu received from the database.
         */
        var databaseMenuItems = [];


        /* =========================================
           PAGE LOAD
        ========================================= */

        var currentMenuMode = "Non-AC";
        var AC_PRICE_MULTIPLIER = 1.05;
        var lastPlacedOrder = null;
        var selectedPaymentMethod = "CASH";
        var paymentProcessing = false;
        var TABLE_BOOKING_CHARGE = 50.0;

        document.addEventListener("DOMContentLoaded", async function () {

            /*
             * First load menu from Spring Boot/MySQL.
             */
            await loadMenuFromDatabase();

            /*
             * Search.
             */
            var searchBox = document.getElementById("dishSearch");

            if (searchBox) {
                searchBox.addEventListener("input", filterDishes);
            }

            /*
             * Initial cart.
             */
            updateCartCount();

        });


        /* =========================================
           LOAD MENU FROM DATABASE
        ========================================= */

        async function loadMenuFromDatabase() {

            var loadingMessage =
                document.getElementById("loadingMessage");

            var menuLayout =
                document.getElementById("menuLayout");

            var sectionNav =
                document.getElementById("sectionNav");

            var sectionContent =
                document.getElementById("sectionContent");

            try {

                console.log("Connecting to database API...");

                /*
                 * Spring Boot API.
                 */
                var response =
                    await fetch(API_BASE_URL + "/api/menu");


                /*
                 * Check API response.
                 */
                if (!response.ok) {

                    throw new Error(
                        "API returned HTTP " + response.status
                    );

                }


                /*
                 * Convert JSON response.
                 */
                var menuItems =
                    await response.json();


                console.log(
                    "Menu loaded from database:",
                    menuItems
                );


                /*
                 * Save globally.
                 */
                databaseMenuItems = menuItems;


                /*
                 * Check if database is empty.
                 */
                if (!Array.isArray(menuItems) ||
                    menuItems.length === 0) {

                    loadingMessage.innerHTML =
                        "No menu items found in the database.";

                    return;
                }


                /*
                 * Clear old generated menu.
                 */
                sectionNav.innerHTML = "";
                sectionContent.innerHTML = "";


                /*
                 * Build menu.
                 */
                buildDynamicMenu(menuItems);


                /*
                 * Hide loading message.
                 */
                loadingMessage.style.display = "none";


                /*
                 * Show menu.
                 */
                menuLayout.style.display = "grid";


                /*
                 * Calculate initial total.
                 */
                calculateTotal();


            } catch (error) {

                console.error(
                    "Error loading menu:",
                    error
                );


                loadingMessage.classList.add(
                    "error-message"
                );

                loadingMessage.innerHTML =
                    "<strong>Unable to load menu</strong><br>" +
                    "Please make sure your Spring Boot backend is running on port 8080." +
                    "<br><br>" +
                    "<small>" +
                    error.message +
                    "</small>";

            }

        }


        /* =========================================
           BUILD MENU FROM DATABASE
        ========================================= */

        function buildDynamicMenu(menuItems) {

            var sectionNav =
                document.getElementById("sectionNav");

            var sectionContent =
                document.getElementById("sectionContent");


            /*
             * Desired main category order.
             */
            var sectionOrder = [

                "Non-Vegetarian Starters",

                "Vegetarian Starters",

                "Vegetarian Main Course",

                "Non-Vegetarian Main Course",

                "Indian Breads",

                "Rice & Noodles",

                "Desserts",

                "Beverages"

            ];


            /*
             * Group database records.
             *
             * Example:
             *
             * Non-Vegetarian Starters - Seafood
             *
             * becomes:
             *
             * Main:
             * Non-Vegetarian Starters
             *
             * Sub:
             * Seafood
             */
            var groupedMenu = {};


            menuItems.forEach(function (item) {

                if (!item.category) {
                    return;
                }

                var categoryText =
                    item.category.trim();


                var parts =
                    categoryText.split(" - ");


                var mainCategory =
                    parts[0].trim();


                var subCategory =
                    parts.length > 1
                        ? parts.slice(1).join(" - ").trim()
                        : "";


                if (!groupedMenu[mainCategory]) {

                    groupedMenu[mainCategory] = {};

                }


                var groupName =
                    subCategory || "__MAIN__";


                if (!groupedMenu[mainCategory][groupName]) {

                    groupedMenu[mainCategory][groupName] = [];

                }


                groupedMenu[mainCategory][groupName].push(item);

            });


            /*
             * Add categories in predefined order.
             */
            var categoriesToShow = [];


            sectionOrder.forEach(function (category) {

                if (groupedMenu[category]) {

                    categoriesToShow.push(category);

                }

            });


            /*
             * Add any new category created by admin
             * that is not in the predefined list.
             */
            Object.keys(groupedMenu).forEach(function (category) {

                if (!categoriesToShow.includes(category)) {

                    categoriesToShow.push(category);

                }

            });


            /*
             * Create every section.
             */
            categoriesToShow.forEach(function (category, index) {

                createMenuSection(
                    category,
                    groupedMenu[category],
                    index
                );

            });

        }


        /* =========================================
           CREATE ONE MENU SECTION
        ========================================= */

        function createMenuSection(
            categoryName,
            subCategories,
            sectionIndex
        ) {

            var sectionNav =
                document.getElementById("sectionNav");

            var sectionContent =
                document.getElementById("sectionContent");


            /*
             * Navigation button.
             */
            var sectionButton =
                document.createElement("button");


            sectionButton.type = "button";

            sectionButton.innerText =
                categoryName;


            /*
             * Section panel.
             */
            var sectionPanel =
                document.createElement("section");


            sectionPanel.className =
                "section-panel";


            /*
             * First category active.
             */
            if (sectionIndex === 0) {

                sectionButton.classList.add("active");

                sectionPanel.classList.add("active");

            }


            /*
             * Main category heading.
             */
            var sectionHeader =
                document.createElement("div");


            sectionHeader.className =
                "Sections";


            var heading =
                document.createElement("h2");


            heading.innerText =
                categoryName;


            sectionHeader.appendChild(heading);

            sectionPanel.appendChild(sectionHeader);


            /*
             * Section heading click.
             *
             * This also allows the section
             * content to be collapsed.
             */
            sectionHeader.addEventListener(
                "click",
                function () {

                    var tables =
                        sectionPanel.querySelectorAll(
                            "table, .sub-category"
                        );


                    var isCollapsed =
                        sectionHeader.classList.contains(
                            "collapsed"
                        );


                    if (isCollapsed) {

                        sectionHeader.classList.remove(
                            "collapsed"
                        );

                        tables.forEach(function (element) {

                            element.style.display = "";

                        });

                    } else {

                        sectionHeader.classList.add(
                            "collapsed"
                        );

                        tables.forEach(function (element) {

                            element.style.display = "none";

                        });

                    }

                }
            );


            /*
             * Sub-category order.
             */
            var subCategoryOrder = [

                "Seafood",

                "Meat",

                "Vegetarian",

                "Non-Vegetarian"

            ];


            var subCategoriesToShow = [];


            subCategoryOrder.forEach(function (sub) {

                if (subCategories[sub]) {

                    subCategoriesToShow.push(sub);

                }

            });


            /*
             * Add any other sub-category
             * added by admin.
             */
            Object.keys(subCategories).forEach(
                function (sub) {

                    if (
                        sub !== "__MAIN__" &&
                        !subCategoriesToShow.includes(sub)
                    ) {

                        subCategoriesToShow.push(sub);

                    }

                }
            );


            /*
             * If there is no sub-category,
             * directly create one table.
             */
            if (subCategories["__MAIN__"]) {

                var mainTable =
                    createMenuTable(
                        subCategories["__MAIN__"]
                    );

                sectionPanel.appendChild(mainTable);

            }


            /*
             * Create sub-category tables.
             */
            subCategoriesToShow.forEach(
                function (subCategory) {

                    var subHeading =
                        document.createElement("h3");

                    subHeading.className =
                        "sub-category";

                    subHeading.innerText =
                        subCategory;


                    sectionPanel.appendChild(
                        subHeading
                    );


                    var table =
                        createMenuTable(
                            subCategories[subCategory]
                        );


                    sectionPanel.appendChild(table);

                }
            );


            /*
             * Navigation button click.
             */
            sectionButton.onclick =
                function () {

                    /*
                     * Remove active from all buttons.
                     */
                    sectionNav
                        .querySelectorAll("button")
                        .forEach(function (button) {

                            button.classList.remove(
                                "active"
                            );

                        });


                    /*
                     * Hide all panels.
                     */
                    sectionContent
                        .querySelectorAll(".section-panel")
                        .forEach(function (panel) {

                            panel.classList.remove(
                                "active"
                            );

                        });


                    /*
                     * Activate selected.
                     */
                    sectionButton.classList.add(
                        "active"
                    );

                    sectionPanel.classList.add(
                        "active"
                    );

                };


            /*
             * Add navigation and panel.
             */
            sectionNav.appendChild(
                sectionButton
            );

            sectionContent.appendChild(
                sectionPanel
            );

        }


        /* =========================================
           CREATE MENU TABLE
        ========================================= */

        function createMenuTable(items) {

            var table =
                document.createElement("table");


            table.className =
                "menu-table";


            /*
             * Sort by database item ID.
             */
            items.sort(function (a, b) {

                return (
                    Number(a.itemId || 0) -
                    Number(b.itemId || 0)
                );

            });


            items.forEach(function (item) {

                /*
                 * Table row.
                 */
                var row =
                    document.createElement("tr");

                var isAvailable = item.available !== false;

                if (!isAvailable) {
                    row.classList.add("out-of-stock-row");
                }


                /*
                 * Store database ID.
                 */
                row.dataset.itemId =
                    item.itemId;


                /*
                 * Dish name.
                 */
                var nameCell =
                    document.createElement("td");


                nameCell.innerText =
                    item.dishName || "Unnamed Dish";

                if (!isAvailable) {
                    var stockLabel = document.createElement("span");
                    stockLabel.className = "out-of-stock-label";
                    stockLabel.innerText = "OUT OF STOCK";
                    nameCell.appendChild(stockLabel);
                }


                /*
                 * Price.
                 */
                var priceCell =
                    document.createElement("td");


                var price =
                    Number(item.price) || 0;

                priceCell.dataset.basePrice = price;
                priceCell.innerHTML = "₹" + getMenuPrice(price);


                /*
                 * Quantity cell.
                 */
                var quantityCell =
                    document.createElement("td");


                /*
                 * Quantity wrapper.
                 */
                var quantityControl =
                    document.createElement("div");


                quantityControl.className =
                    "quantity-control";


                /*
                 * Minus button.
                 */
                var minusButton =
                    document.createElement("button");


                minusButton.type =
                    "button";

                minusButton.className =
                    "qty-btn";

                minusButton.innerHTML =
                    "−";


                minusButton.onclick =
                    function () {

                        changeQuantity(
                            this,
                            -1
                        );

                    };


                /*
                 * Quantity input.
                 */
                var quantityInput =
                    document.createElement("input");


                quantityInput.type =
                    "number";

                quantityInput.className =
                    "qty-input";

                quantityInput.value =
                    "0";

                quantityInput.min =
                    "0";


                quantityInput.onchange =
                    function () {

                        calculateTotal();

                    };


                /*
                 * Plus button.
                 */
                var plusButton =
                    document.createElement("button");


                plusButton.type =
                    "button";

                plusButton.className =
                    "qty-btn";

                plusButton.innerHTML =
                    "+";


                plusButton.onclick =
                    function () {

                        changeQuantity(
                            this,
                            1
                        );

                    };


                /*
                 * Assemble quantity control.
                 */
                quantityControl.appendChild(
                    minusButton
                );

                quantityControl.appendChild(
                    quantityInput
                );

                quantityControl.appendChild(
                    plusButton
                );


                quantityCell.appendChild(
                    quantityControl
                );

                if (!isAvailable) {
                    minusButton.disabled = true;
                    quantityInput.disabled = true;
                    plusButton.disabled = true;
                    quantityInput.value = "0";
                }


                /*
                 * Assemble row.
                 */
                row.appendChild(
                    nameCell
                );

                row.appendChild(
                    priceCell
                );

                row.appendChild(
                    quantityCell
                );


                /*
                 * Add row to table.
                 */
                table.appendChild(
                    row
                );

            });


            return table;

        }


        /* =========================================
           CHANGE QUANTITY
        ========================================= */

        function changeQuantity(button, change) {

            var input =
                button.parentElement.querySelector(
                    ".qty-input"
                );


            var value =
                Number(input.value) || 0;


            value += change;


            if (value < 0) {

                value = 0;

            }


            input.value =
                value;


            calculateTotal();

        }


        /* =========================================
           AC / NON-AC MENU MODE
        ========================================= */

        function getMenuPrice(basePrice) {
            var price = Number(basePrice) || 0;

            if (currentMenuMode === "AC") {
                return Math.round(price * AC_PRICE_MULTIPLIER);
            }

            return price;
        }

        function setMenuMode(mode) {
            currentMenuMode = mode === "AC" ? "AC" : "Non-AC";

            document.body.classList.toggle(
                "ac-mode",
                currentMenuMode === "AC"
            );

            var acButton = document.getElementById("acModeButton");
            var nonAcButton = document.getElementById("nonAcModeButton");
            var note = document.getElementById("menuModeNote");

            if (acButton) {
                acButton.classList.toggle(
                    "active",
                    currentMenuMode === "AC"
                );
            }

            if (nonAcButton) {
                nonAcButton.classList.toggle(
                    "active",
                    currentMenuMode === "Non-AC"
                );
            }

            if (note) {
                note.innerText = currentMenuMode === "AC"
                    ? "AC prices +5%"
                    : "Normal menu prices";
            }

            updateAllMenuPrices();

            /*
             * Room charge is automatically calculated from this
             * AC / Non-AC mode. No separate room selector is needed.
             */
            updateBillBreakdown();
        }

        function updateAllMenuPrices() {
            document.querySelectorAll(".menu-table tr").forEach(function(row) {
                var priceCell = row.cells[1];
                if (!priceCell) return;

                var basePrice = Number(priceCell.dataset.basePrice);
                if (!Number.isFinite(basePrice)) return;

                priceCell.innerHTML =
                    "₹" + getMenuPrice(basePrice) +
                    (currentMenuMode === "AC"
                        ? '<span class="ac-price-badge">AC</span>'
                        : "");
            });

            calculateTotal();
        }

        /* =========================================
           CALCULATE TOTAL
        ========================================= */

        function calculateTotal() {

            var total = 0;


            var rows =
                document.querySelectorAll(
                    ".menu-table tr"
                );


            rows.forEach(function (row) {

                var priceCell =
                    row.cells[1];


                var qtyInput =
                    row.querySelector(
                        ".qty-input"
                    );


                if (
                    priceCell &&
                    qtyInput
                ) {

                    /*
                     * Remove ₹ and any other symbols.
                     */
                    var price = Number(
                        priceCell.dataset.basePrice
                    ) || 0;

                    price = getMenuPrice(price);


                    var quantity =
                        Number(
                            qtyInput.value
                        ) || 0;


                    total +=
                        price * quantity;

                }

            });


            document.getElementById(
                "totalAmount"
            ).innerText =
                total;


            updateCartCount();


            return total;

        }


        /* =========================================
           UPDATE CART COUNT
        ========================================= */

        function updateCartCount() {

            var itemCount = 0;


            document
                .querySelectorAll(".qty-input")
                .forEach(function (input) {

                    itemCount +=
                        Number(input.value) || 0;

                });


            document.getElementById(
                "itemCount"
            ).innerText =
                itemCount;

        }


        /* =========================================
           SEARCH DISHES
        ========================================= */

        function filterDishes() {

            var query =
                document.getElementById(
                    "dishSearch"
                ).value
                .trim()
                .toLowerCase();


            var matchCount = 0;


            document
                .querySelectorAll(".section-panel")
                .forEach(function (panel) {

                    var panelHasMatch = false;


                    panel
                        .querySelectorAll(".menu-table")
                        .forEach(function (table) {

                            var tableHasMatch =
                                false;


                            table
                                .querySelectorAll("tr")
                                .forEach(function (row) {

                                    var nameCell =
                                        row.cells[0];


                                    var matches =
                                        !query ||
                                        (
                                            nameCell &&
                                            nameCell.innerText
                                                .toLowerCase()
                                                .includes(query)
                                        );


                                    row.style.display =
                                        matches
                                            ? ""
                                            : "none";


                                    if (matches) {

                                        tableHasMatch =
                                            true;

                                        panelHasMatch =
                                            true;

                                        matchCount++;

                                    }

                                });


                            table.style.display =
                                tableHasMatch
                                    ? ""
                                    : "none";

                        });


                    panel.classList.toggle(
                        "search-visible",
                        Boolean(query) &&
                        panelHasMatch
                    );

                });


            /*
             * When search is active,
             * show matching panels.
             */
            if (query) {

                document
                    .querySelectorAll(".section-panel")
                    .forEach(function (panel) {

                        var visibleRows =
                            panel.querySelectorAll(
                                ".menu-table tr:not([style*='display: none'])"
                            );


                        if (visibleRows.length > 0) {

                            panel.classList.add(
                                "search-visible"
                            );

                        } else {

                            panel.classList.remove(
                                "search-visible"
                            );

                        }

                    });

            }


            /*
             * No dishes message.
             */
            document.getElementById(
                "noDishesMessage"
            ).style.display =
                query && matchCount === 0
                    ? "block"
                    : "none";

        }


        /* =========================================
           BILL BREAKDOWN
        ========================================= */

        function getBillBreakdown() {

            var foodSubtotal =
                calculateTotal();


            var orderTypeInput =
                document.querySelector(
                    'input[name="orderType"]:checked'
                );


            var orderType =
                orderTypeInput
                    ? orderTypeInput.value
                    : "Dine In";


            /*
             * The Order Summary no longer asks the customer to
             * select a room type. The AC / Non-AC menu mode at the
             * top of the page controls the room charge automatically.
             * Non-AC = ₹50, AC = ₹100. Take Away = ₹0.
             */
            var roomCharge =
                orderType === "Dine In"
                    ? (currentMenuMode === "AC" ? 100 : 50)
                    : 0;


            var taxableAmount =
                foodSubtotal +
                roomCharge;


            var gst =
                Math.round(
                    taxableAmount *
                    0.05 *
                    100
                ) / 100;


            return {

                foodSubtotal:
                    foodSubtotal,

                roomCharge:
                    roomCharge,

                gst:
                    gst,

                total:
                    Math.round(
                        (
                            taxableAmount +
                            gst
                        ) * 100
                    ) / 100

            };

        }


        /* =========================================
           UPDATE BILL
        ========================================= */

        function updateBillBreakdown() {

            var bill =
                getBillBreakdown();


            document.getElementById(
                "foodSubtotal"
            ).innerText =
                bill.foodSubtotal.toFixed(2);


            document.getElementById(
                "roomCharge"
            ).innerText =
                bill.roomCharge.toFixed(2);


            document.getElementById(
                "gstAmount"
            ).innerText =
                bill.gst.toFixed(2);


            document.getElementById(
                "summaryTotal"
            ).innerText =
                bill.total.toFixed(2);

        }


        /* =========================================
           ORDER TYPE
        ========================================= */

        function updateOrderTypeOptions() {

            /*
             * Room type selection was removed from the Order Summary.
             * Dine In uses the current AC / Non-AC dining mode.
             */
            updateBillBreakdown();

        }


        /* =========================================
           SHOW MENU
        ========================================= */

        function showMenu() {

            document.getElementById("menuSection").style.display = "block";
            document.getElementById("tableBookingSection").style.display = "none";
            document.getElementById("checkOrderSection").style.display = "none";

            var menuTools = document.querySelector(".menu-tools");
            var foodShowcase = document.querySelector(".food-showcase");

            if (menuTools) menuTools.style.display = "flex";
            if (foodShowcase) foodShowcase.style.display = "grid";

            document.getElementById("summaryPopup").style.display = "none";
            document.getElementById("customerPopup").style.display = "none";
            document.getElementById("paymentPopup").style.display = "none";
            document.getElementById("confirmPopup").style.display = "none";
        }



        /* =========================================
           SHOW CHECK ORDER
        ========================================= */

        function showCheckOrder() {

            document.getElementById("menuSection").style.display = "none";
            document.getElementById("tableBookingSection").style.display = "none";
            document.getElementById("checkOrderSection").style.display = "block";

            var menuTools = document.querySelector(".menu-tools");
            var foodShowcase = document.querySelector(".food-showcase");

            if (menuTools) menuTools.style.display = "none";
            if (foodShowcase) foodShowcase.style.display = "none";

            document.getElementById("trackResult").innerHTML = "";
            document.getElementById("trackNumber").value = "";
        }


        /* =========================================
           SHOW TABLE BOOKING
        ========================================= */

        function showTableBooking() {

            document.getElementById("menuSection").style.display = "none";
            document.getElementById("checkOrderSection").style.display = "none";
            document.getElementById("tableBookingSection").style.display = "block";

            var menuTools = document.querySelector(".menu-tools");
            var foodShowcase = document.querySelector(".food-showcase");

            if (menuTools) menuTools.style.display = "none";
            if (foodShowcase) foodShowcase.style.display = "none";

            /* Close order/payment popups when opening table booking. */
            document.getElementById("summaryPopup").style.display = "none";
            document.getElementById("customerPopup").style.display = "none";
            document.getElementById("paymentPopup").style.display = "none";
            document.getElementById("confirmPopup").style.display = "none";

            initializeTableBookingForm();
        }



        /* =========================================
           SHOW ORDER SUMMARY
        ========================================= */

        function showOrderSummary() {

            var total =
                calculateTotal();


            if (total === 0) {

                alert(
                    "Please select at least one dish"
                );

                return;

            }


            var summaryText =
                "";


            var rows =
                document.querySelectorAll(
                    ".menu-table tr"
                );


            rows.forEach(function (row) {

                var nameCell =
                    row.cells[0];


                var qtyInput =
                    row.querySelector(
                        ".qty-input"
                    );


                if (
                    nameCell &&
                    qtyInput
                ) {

                    var qty =
                        Number(
                            qtyInput.value
                        ) || 0;


                    if (qty > 0) {

                        var name =
                            nameCell.innerText;


                        summaryText +=
                            name +
                            " x " +
                            qty +
                            "<br>";

                    }

                }

            });


            document.getElementById(
                "summaryText"
            ).innerHTML =
                summaryText;


            updateOrderTypeOptions();


            document.getElementById(
                "summaryPopup"
            ).style.display =
                "block";

        }


        /* =========================================
           CLOSE SUMMARY
        ========================================= */

        function closeSummaryPopup() {

            document.getElementById(
                "summaryPopup"
            ).style.display =
                "none";

        }


        /* =========================================
           OPEN CUSTOMER POPUP
        ========================================= */

        function openCustomerPopup() {

            document.getElementById(
                "summaryPopup"
            ).style.display =
                "none";

            var phone = document.getElementById("custPhone").value.trim();
            if (phone && /^\d{10}$/.test(phone)) {
                try {
                    var savedReservation = localStorage.getItem("gomantakLastReservation_" + phone);
                    if (savedReservation) {
                        document.getElementById("custReservationNumber").value = savedReservation;
                    }
                } catch (ignore) {}
            }

            document.getElementById(
                "customerPopup"
            ).style.display =
                "block";

        }


        /* =========================================
           CLOSE CUSTOMER POPUP
        ========================================= */

        function closeCustomerPopup() {

            document.getElementById(
                "customerPopup"
            ).style.display =
                "none";


            document.getElementById(
                "summaryPopup"
            ).style.display =
                "block";

        }


        /* =========================================
           PLACE ORDER
        ========================================= */

        /* =========================================
           TABLE BOOKING
        ========================================= */

        var TABLE_RESERVATION_API = API_BASE_URL + "/api/table-reservations";
        var availableBookingTables = [];
        var selectedBookingTableId = null;
        var bookingFormInitialized = false;

        function initializeTableBookingForm() {
            if (bookingFormInitialized) return;
            bookingFormInitialized = true;

            var dateInput = document.getElementById("bookingDate");
            var today = new Date();
            var yyyy = today.getFullYear();
            var mm = String(today.getMonth() + 1).padStart(2, "0");
            var dd = String(today.getDate()).padStart(2, "0");
            var todayValue = yyyy + "-" + mm + "-" + dd;
            dateInput.min = todayValue;
            dateInput.value = todayValue;

            var timeInput = document.getElementById("bookingTime");
            if (!timeInput.value) timeInput.value = "19:00";
        }

        async function checkTableAvailability() {
            initializeTableBookingForm();

            var date = document.getElementById("bookingDate").value;
            var time = document.getElementById("bookingTime").value;
            var guests = Number(document.getElementById("bookingGuests").value);
            var error = document.getElementById("bookingError");
            var list = document.getElementById("availableTableList");
            var select = document.getElementById("bookingTable");

            error.textContent = "";
            list.innerHTML = "";
            select.innerHTML = '<option value="">Select a table</option>';
            select.disabled = true;
            selectedBookingTableId = null;

            if (!date || !time) { error.textContent = "Please select a date and time."; return; }
            if (!guests || guests < 1 || guests > 20) { error.textContent = "Guests must be between 1 and 20."; return; }

            try {
                var response = await fetch(TABLE_RESERVATION_API + "/available-tables?date=" + encodeURIComponent(date) + "&time=" + encodeURIComponent(time) + "&guests=" + guests);
                if (!response.ok) {
                    var text = await response.text();
                    throw new Error(text || "Could not check table availability");
                }

                availableBookingTables = await response.json();

                if (!availableBookingTables.length) {
                    list.innerHTML = "<p>No table is available for the selected time. Try another time.</p>";
                    return;
                }

                availableBookingTables.forEach(function(table) {
                    var card = document.createElement("div");
                    card.className = "available-table-option";
                    card.setAttribute("data-table-id", table.tableId);
                    card.innerHTML = "<strong>🪑 " + escapeBookingHtml(table.tableNumber) + "</strong><span>Capacity: " + table.capacity + " guests</span><br><small>Section: " + escapeBookingHtml(table.section) + "</small>";
                    card.onclick = function(){ selectBookingTable(table.tableId); };
                    list.appendChild(card);

                    var option = document.createElement("option");
                    option.value = table.tableId;
                    option.textContent = table.tableNumber + " · " + table.capacity + " seats · " + table.section;
                    select.appendChild(option);
                });

                select.disabled = false;
                select.onchange = function(){ selectBookingTable(Number(this.value)); };
                selectBookingTable(availableBookingTables[0].tableId);
            } catch (e) {
                console.error(e);
                error.textContent = e.message || "Could not check table availability.";
            }
        }

        function selectBookingTable(tableId) {
            selectedBookingTableId = Number(tableId);
            var select = document.getElementById("bookingTable");
            select.value = String(selectedBookingTableId);
            document.querySelectorAll(".available-table-option").forEach(function(card){
                card.classList.toggle("selected", Number(card.getAttribute("data-table-id")) === selectedBookingTableId);
            });
        }

        async function confirmTableBooking() {
            var name = document.getElementById("bookingName").value.trim();
            var phone = document.getElementById("bookingPhone").value.trim();
            var date = document.getElementById("bookingDate").value;
            var time = document.getElementById("bookingTime").value;
            var guests = Number(document.getElementById("bookingGuests").value);
            var request = document.getElementById("bookingRequest").value.trim();
            var error = document.getElementById("bookingError");
            var result = document.getElementById("bookingResult");

            error.textContent = "";
            result.style.display = "none";

            if (!name) { error.textContent = "Please enter your name."; return; }
            if (!/^\d{10}$/.test(phone)) { error.textContent = "Please enter a valid 10-digit mobile number."; return; }
            if (!date || !time) { error.textContent = "Please select the booking date and time."; return; }
            if (!guests || guests < 1 || guests > 20) { error.textContent = "Guests must be between 1 and 20."; return; }
            if (!selectedBookingTableId) { error.textContent = "Please check availability and select a table."; return; }

            try {
                var response = await fetch(TABLE_RESERVATION_API, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        customerName: name,
                        phone: phone,
                        table: { tableId: selectedBookingTableId },
                        reservationDate: date,
                        reservationTime: time,
                        guestCount: guests,
                        specialRequest: request
                    })
                });

                if (!response.ok) {
                    var text = await response.text();
                    throw new Error(text || "Booking failed");
                }

                var saved = await response.json();
                try {
                    localStorage.setItem("gomantakLastReservation_" + phone, saved.reservationNumber);
                } catch (ignore) {}
                var table = availableBookingTables.find(function(t){ return Number(t.tableId) === selectedBookingTableId; });
                result.className = "booking-result success";
                result.innerHTML = "<strong>🎉 Table booked successfully!</strong><br><br>Reservation: <strong>" + escapeBookingHtml(saved.reservationNumber) + "</strong><br>Table: <strong>" + escapeBookingHtml(table ? table.tableNumber : "Selected table") + "</strong><br>Date: " + escapeBookingHtml(saved.reservationDate) + "<br>Time: " + escapeBookingHtml(String(saved.reservationTime).slice(0,5)) + "<br>Guests: " + saved.guestCount + "<br>Status: <strong>CONFIRMED</strong>";
                result.style.display = "block";

                selectedBookingTableId = null;
                document.getElementById("availableTableList").innerHTML = "";
                document.getElementById("bookingTable").innerHTML = '<option value="">Booking completed</option>';
                document.getElementById("bookingTable").disabled = true;
            } catch (e) {
                console.error(e);
                error.textContent = e.message || "Could not complete the booking.";
            }
        }

        function resetTableBookingForm() {
            document.getElementById("bookingName").value = "";
            document.getElementById("bookingPhone").value = "";
            document.getElementById("bookingGuests").value = "2";
            document.getElementById("bookingRequest").value = "";
            document.getElementById("bookingError").textContent = "";
            document.getElementById("availableTableList").innerHTML = "";
            document.getElementById("bookingTable").innerHTML = '<option value="">First check availability</option>';
            document.getElementById("bookingTable").disabled = true;
            document.getElementById("bookingResult").style.display = "none";
            selectedBookingTableId = null;
            initializeTableBookingForm();
        }

        function escapeBookingHtml(value) {
            return String(value == null ? "" : value).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&#39;");
        }

        async function placeOrder() {

    var name = document.getElementById("custName").value.trim();
    var phone = document.getElementById("custPhone").value.trim();

    if (name === "") {
        alert("Please enter your name");
        return;
    }

    if (phone === "") {
        alert("Please enter your phone number");
        return;
    }

    if (!/^\d{10}$/.test(phone)) {
        alert("Please enter a valid 10-digit phone number");
        return;
    }

    var orderType = document.querySelector('input[name="orderType"]:checked').value;

    var roomType = orderType === "Dine In"
        ? currentMenuMode
        : "Not Applicable";

    var bill = getBillBreakdown();

    if (bill.foodSubtotal <= 0) {
        alert("Please select at least one item");
        return;
    }

    // Collect selected menu items
    var items = [];

    document.querySelectorAll(".menu-table tr").forEach(function(row) {

        var input = row.querySelector(".qty-input");

        if (!input) return;

        var quantity = Number(input.value) || 0;

        if (quantity > 0) {

            var itemId = row.getAttribute("data-item-id");

            if (itemId) {
                items.push({
                    itemId: Number(itemId),
                    quantity: quantity
                });
            }
        }
    });

    if (items.length === 0) {
        alert("Please select at least one item");
        return;
    }

    // Data sent to Spring Boot
    var reservationNumber = document.getElementById("custReservationNumber").value.trim().toUpperCase();

    var orderData = {
        customerName: name,
        phone: phone,
        orderType: orderType,
        roomType: roomType,
        reservationNumber: reservationNumber || null,
        items: items
    };

    try {

        var response = await fetch(API_BASE_URL + "/api/orders", {

            method: "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify(orderData)
        });

        if (!response.ok) {

            var errorText = await response.text();

            console.error(errorText);

            alert("Failed to place order.");
            return;
        }

        var order = await response.json();

        console.log("Order saved in database:", order);
        lastPlacedOrder = order;
        selectedPaymentMethod = "CASH";
        paymentProcessing = false;

        document.getElementById("customerPopup").style.display = "none";
        document.getElementById("paymentOrderText").innerHTML = "Order <strong>" + escapeBookingHtml(order.orderNumber) + "</strong> has been created.";
        document.getElementById("paymentTotal").innerText = Number(order.totalAmount || 0).toFixed(2);
        document.getElementById("paymentStatusLine").innerHTML = "Payment Status: <strong>PENDING</strong>";
        selectPaymentMethod("CASH");
        document.getElementById("paymentPopup").style.display = "block";

    } catch (error) {

        console.error("Order error:", error);

        alert("Cannot connect to the Spring Boot server. Make sure it is running on port 8080.");
    }
}


        /* =========================================
           PAYMENT
        ========================================= */

        function selectPaymentMethod(method) {
            selectedPaymentMethod = method;
            ["CASH", "UPI", "CARD"].forEach(function(value) {
                var id = value === "CASH" ? "paymentCashButton" : (value === "UPI" ? "paymentUpiButton" : "paymentCardButton");
                var button = document.getElementById(id);
                if (button) button.classList.toggle("active", value === method);
            });
        }

        async function completePayment() {
            if (!lastPlacedOrder || !lastPlacedOrder.orderNumber || paymentProcessing) return;

            paymentProcessing = true;
            document.getElementById("paymentStatusLine").innerHTML = "Processing <strong>" + selectedPaymentMethod + "</strong> payment...";

            try {
                var response = await fetch(
                    "http://localhost:8080/api/orders/" + encodeURIComponent(lastPlacedOrder.orderNumber) + "/payment",
                    {
                        method: "PATCH",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            paymentStatus: "PAID",
                            paymentMethod: selectedPaymentMethod
                        })
                    }
                );

                if (!response.ok) {
                    var errorText = await response.text();
                    throw new Error(errorText || "Payment update failed");
                }

                lastPlacedOrder = await response.json();
                document.getElementById("paymentPopup").style.display = "none";
                showOrderConfirmation(lastPlacedOrder);
            } catch (error) {
                console.error("Payment error:", error);
                document.getElementById("paymentStatusLine").innerHTML = "<span style='color:#b00020;'>Payment failed. Please try again.</span>";
            } finally {
                paymentProcessing = false;
            }
        }

        function payLater() {
            if (!lastPlacedOrder) return;
            document.getElementById("paymentPopup").style.display = "none";
            showOrderConfirmation(lastPlacedOrder);
        }

        function showOrderConfirmation(order) {
            var paymentText = String(order.paymentStatus || "PENDING").toUpperCase();
            var methodText = String(order.paymentMethod || "NOT_SELECTED").replace("_", " ");
            var bookingLine = Number(order.bookingCharge || 0) > 0
                ? "<br>Table Booking Charge: <b>₹" + Number(order.bookingCharge).toFixed(2) + "</b>"
                : "";

            document.getElementById("confirmText").innerHTML =
                "Thank you " + escapeBookingHtml(order.customerName || "Customer") +
                "! Your order has been placed." +
                "<br><br>Your Order Number is: <b>" + escapeBookingHtml(order.orderNumber) + "</b>" +
                bookingLine +
                "<br>Total Bill: <b>₹" + Number(order.totalAmount || 0).toFixed(2) + "</b>" +
                "<br>Payment: <b>" + escapeBookingHtml(paymentText) + "</b>" +
                (paymentText === "PAID" ? " via <b>" + escapeBookingHtml(methodText) + "</b>" : "") +
                "<br><br>Please note your order number to track your order." +
                "<br><button class='bill-print-button' onclick='printBill(lastPlacedOrder)'>🧾 Print Bill</button>";

            document.getElementById("confirmPopup").style.display = "block";
        }

        function printBill(order) {
            if (!order) return;
            var itemsHtml = (order.items || []).map(function(item) {
                return "<tr><td>" + escapeBookingHtml(item.dishName) + "</td><td>" + item.quantity + "</td><td>₹" + Number(item.subtotal || 0).toFixed(2) + "</td></tr>";
            }).join("");
            var booking = Number(order.bookingCharge || 0);
            var html = "<html><head><title>Bill " + escapeBookingHtml(order.orderNumber) + "</title><style>body{font-family:Arial,sans-serif;padding:30px;max-width:650px;margin:auto}h1{text-align:center}table{width:100%;border-collapse:collapse;margin:20px 0}th,td{padding:8px;border-bottom:1px solid #ddd;text-align:left}.right{text-align:right}</style></head><body>" +
                "<h1>Gomantak Saguti Restaurant</h1><p><b>Order:</b> " + escapeBookingHtml(order.orderNumber) + "<br><b>Customer:</b> " + escapeBookingHtml(order.customerName) + "<br><b>Phone:</b> " + escapeBookingHtml(order.phone) + "</p>" +
                "<table><thead><tr><th>Item</th><th>Qty</th><th>Amount</th></tr></thead><tbody>" + itemsHtml + "</tbody></table>" +
                "<p class='right'>Food Subtotal: ₹" + Number(order.foodSubtotal || 0).toFixed(2) + "<br>Room Charge: ₹" + Number(order.roomCharge || 0).toFixed(2) + "<br>Table Booking: ₹" + booking.toFixed(2) + "<br>GST (5%): ₹" + Number(order.gst || 0).toFixed(2) + "<br><b>Total: ₹" + Number(order.totalAmount || 0).toFixed(2) + "</b><br>Payment: " + escapeBookingHtml(order.paymentStatus || "PENDING") + (order.paymentMethod && order.paymentMethod !== "NOT_SELECTED" ? " (" + escapeBookingHtml(order.paymentMethod) + ")" : "") + "</p></body></html>";
            var w = window.open("", "_blank");
            if (!w) { alert("Please allow pop-ups to print the bill."); return; }
            w.document.write(html);
            w.document.close();
            w.focus();
            setTimeout(function(){ w.print(); }, 250);
        }


        /* =========================================
           CLOSE CONFIRMATION
        ========================================= */

        function closeConfirmPopup() {

            document.getElementById(
                "confirmPopup"
            ).style.display =
                "none";


            /*
             * Reset quantities.
             */
            var inputs =
                document.querySelectorAll(
                    ".qty-input"
                );


            inputs.forEach(function (input) {

                input.value =
                    0;

            });


            calculateTotal();


            /*
             * Clear customer details.
             */
            document.getElementById(
                "custName"
            ).value =
                "";


            document.getElementById(
                "custPhone"
            ).value =
                "";

            document.getElementById(
                "custReservationNumber"
            ).value =
                "";

            lastPlacedOrder = null;

        }


        /* =========================================
           TRACK ORDER
        ========================================= */

       async function trackOrder() {

    var enteredNumber = document.getElementById("trackNumber").value.trim();

    if (enteredNumber === "") {
        alert("Please enter your order number");
        return;
    }

    try {

        var response = await fetch(API_BASE_URL + "/api/orders/" + encodeURIComponent(enteredNumber));

        if (!response.ok) {

            document.getElementById("trackResult").innerHTML =
                "<p style='color:red;'>Order number not found. Please check and try again.</p>";

            return;
        }

        var order = await response.json();

        var resultText =
            "<h3>Order Number: " + order.orderNumber + "</h3>";

        resultText +=
            "<p><strong>Name:</strong> " +
            order.customerName + "</p>";

        resultText +=
            "<p><strong>Phone:</strong> " +
            order.phone + "</p>";

        resultText +=
            "<p><strong>Order Type:</strong> " +
            order.orderType + "</p>";

        resultText +=
            "<p><strong>Room Type:</strong> " +
            order.roomType + "</p>";

        resultText += "<hr>";

        resultText += "<h4>Items:</h4>";

        order.items.forEach(function(item) {

            resultText +=
                "<p>" +
                item.dishName +
                " × " +
                item.quantity +
                " = ₹" +
                Number(item.subtotal).toFixed(2) +
                "</p>";

        });

        resultText += "<hr>";

        resultText +=
            "<p><strong>Food Subtotal:</strong> ₹" +
            Number(order.foodSubtotal).toFixed(2) +
            "</p>";

        resultText +=
            "<p><strong>Room Charge:</strong> ₹" +
            Number(order.roomCharge).toFixed(2) +
            "</p>";

        resultText +=
            "<p><strong>GST (5%):</strong> ₹" +
            Number(order.gst).toFixed(2) +
            "</p>";

        if (Number(order.bookingCharge || 0) > 0) {
            resultText +=
                "<p><strong>Table Booking Charge:</strong> ₹" +
                Number(order.bookingCharge).toFixed(2) +
                (order.reservationNumber ? " (" + escapeBookingHtml(order.reservationNumber) + ")" : "") +
                "</p>";
        }

        resultText +=
            "<p><strong>Total Bill:</strong> ₹" +
            Number(order.totalAmount).toFixed(2) +
            "</p>";

        resultText +=
            "<p><strong>Payment:</strong> " +
            escapeBookingHtml(order.paymentStatus || "PENDING") +
            (order.paymentMethod && order.paymentMethod !== "NOT_SELECTED" ? " via " + escapeBookingHtml(order.paymentMethod) : "") +
            "</p>";

        resultText +=
            "<p><strong>Status:</strong> " +
            escapeBookingHtml(order.status) +
            "</p>" +
            "<button class='bill-print-button' onclick='printBill(" + JSON.stringify(order).replace(/</g,"\u003c") + ")'>🧾 Print Bill</button>";

        document.getElementById("trackResult").innerHTML = resultText;

    } catch (error) {

        console.error("Tracking error:", error);

        document.getElementById("trackResult").innerHTML =
            "<p style='color:red;'>Cannot connect to the server.</p>";
    }
}
