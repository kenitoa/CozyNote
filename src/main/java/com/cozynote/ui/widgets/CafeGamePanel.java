package com.cozynote.ui.widgets;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public final class CafeGamePanel extends VBox {
    private static final double PANEL_WIDTH = 388;
    private static final double PANEL_HEIGHT = 400;
    private static final double SCENE_HEIGHT = 258;
    private static final int MAX_LOGS = 6;
    private static final double ORDER_THRESHOLD = 3.0;
    private static final double CUSTOMER_THRESHOLD = 4.0;
    private static final double SHOP_CARD_Y = 209;
    private static final double SHOP_CARD_HEIGHT = 44;
    private static final double SHOP_ITEM_SIZE = 16;
    private static final int ITEM_OBJECT_COST = 100;
    private static final int TIME_OBJECT_COST = 150;
    private static final int SURFACE_OBJECT_COST = 50;

    private static final Font SMALL_FONT = Font.font("Malgun Gothic", 10);
    private static final Font SMALL_BOLD_FONT = Font.font("Malgun Gothic", FontWeight.BOLD, 10);
    private static final Font LABEL_FONT = Font.font("Malgun Gothic", FontWeight.BOLD, 11);

    private static final AvatarPalette[] CUSTOMER_PALETTES = new AvatarPalette[] {
            new AvatarPalette(Color.rgb(116, 73, 45), Color.rgb(234, 210, 183), Color.rgb(112, 81, 59), Accessory.NONE),
            new AvatarPalette(Color.rgb(47, 41, 43), Color.rgb(68, 93, 128), Color.rgb(82, 92, 102), Accessory.NONE),
            new AvatarPalette(Color.rgb(110, 71, 46), Color.rgb(213, 156, 166), Color.rgb(84, 106, 130), Accessory.NONE),
            new AvatarPalette(Color.rgb(67, 49, 39), Color.rgb(108, 136, 84), Color.rgb(62, 71, 78), Accessory.GLASSES),
            new AvatarPalette(Color.rgb(54, 67, 84), Color.rgb(71, 112, 153), Color.rgb(66, 86, 116), Accessory.CAP),
            new AvatarPalette(Color.rgb(112, 79, 48), Color.rgb(233, 189, 84), Color.rgb(93, 104, 74), Accessory.BUN),
            new AvatarPalette(Color.rgb(118, 74, 52), Color.rgb(240, 238, 232), Color.rgb(79, 105, 136), Accessory.NONE),
            new AvatarPalette(Color.rgb(170, 170, 176), Color.rgb(55, 55, 59), Color.rgb(56, 60, 72), Accessory.NONE)
    };

    private static final AvatarPalette[] STAFF_PALETTES = new AvatarPalette[] {
            new AvatarPalette(Color.rgb(87, 63, 43), Color.rgb(240, 237, 232), Color.rgb(55, 55, 60), Accessory.APRON),
            new AvatarPalette(Color.rgb(108, 72, 48), Color.rgb(231, 209, 194), Color.rgb(55, 55, 60), Accessory.APRON),
            new AvatarPalette(Color.rgb(229, 196, 127), Color.rgb(240, 237, 232), Color.rgb(55, 55, 60), Accessory.PONYTAIL)
    };

    private static final WindowTheme[] WINDOW_THEMES = new WindowTheme[] {
            new WindowTheme("AM", Color.rgb(248, 213, 148), Color.rgb(233, 245, 255), Color.rgb(242, 197, 119), false),
            new WindowTheme("DAY", Color.rgb(111, 178, 230), Color.rgb(209, 239, 255), Color.rgb(255, 255, 255), false),
            new WindowTheme("PM", Color.rgb(215, 117, 72), Color.rgb(255, 206, 138), Color.rgb(255, 238, 199), false),
            new WindowTheme("NIGHT", Color.rgb(25, 43, 79), Color.rgb(14, 24, 44), Color.rgb(236, 219, 133), true)
    };

    private static final SurfacePalette[] FLOOR_STYLES = new SurfacePalette[] {
            new SurfacePalette(Color.rgb(132, 88, 53), Color.rgb(164, 114, 72)),
            new SurfacePalette(Color.rgb(121, 78, 47), Color.rgb(157, 107, 64)),
            new SurfacePalette(Color.rgb(106, 70, 43), Color.rgb(137, 94, 60)),
            new SurfacePalette(Color.rgb(143, 95, 57), Color.rgb(176, 124, 78)),
            new SurfacePalette(Color.rgb(112, 92, 74), Color.rgb(132, 116, 102))
    };

    private static final SurfacePalette[] WALL_STYLES = new SurfacePalette[] {
            new SurfacePalette(Color.rgb(109, 68, 45), Color.rgb(82, 51, 32)),
            new SurfacePalette(Color.rgb(235, 221, 194), Color.rgb(244, 233, 212)),
            new SurfacePalette(Color.rgb(96, 110, 93), Color.rgb(122, 138, 120)),
            new SurfacePalette(Color.rgb(231, 211, 184), Color.rgb(243, 224, 201)),
            new SurfacePalette(Color.rgb(224, 212, 196), Color.rgb(208, 194, 176)),
            new SurfacePalette(Color.rgb(210, 191, 163), Color.rgb(231, 216, 191))
    };

    private static final String[] BUBBLE_LABELS = {"LOVE", "...", "COFF", "MUSI"};

    private static final InteriorUnlock[] INTERIOR_UNLOCKS = new InteriorUnlock[] {
            new InteriorUnlock("Pendant Lamp", 40),
            new InteriorUnlock("Plant Shelf", 65),
            new InteriorUnlock("Wall Frame", 90),
            new InteriorUnlock("Cabinet", 120),
            new InteriorUnlock("Radio", 150),
            new InteriorUnlock("Sofa", 190),
            new InteriorUnlock("Window Theme", 230),
            new InteriorUnlock("Floor Finish", 270),
            new InteriorUnlock("Wallpaper", 320)
    };

    private final Canvas canvas = new Canvas(PANEL_WIDTH, SCENE_HEIGHT);
    private final Label summaryLabel = new Label();
    private final Label levelLabel = new Label();
    private final Button coffeeUpgradeButton = new Button();
    private final Button staffUpgradeButton = new Button();
    private final Button interiorUpgradeButton = new Button();
    private final ObservableList<String> logs = FXCollections.observableArrayList();
    private final ListView<String> logListView = new ListView<>(logs);
    private final Timeline gameTimer;

    private int coins = 30;
    private int queue = 3;
    private int batchProgress = 0;
    private int totalCustomersServed = 0;
    private int totalRevenue = 0;
    private int comboStreak = 0;
    private int coffeeMachineLevel = 1;
    private int staffSpeedLevel = 1;
    private int interiorLevel = 1;
    private int selectedThemeIndex = 0;
    private int selectedFloorIndex = 0;
    private int selectedWallIndex = 0;
    private final boolean[] itemOwned = new boolean[8];
    private final boolean[] timeOwned = new boolean[4];
    private final boolean[] floorOwned = new boolean[5];
    private final boolean[] wallOwned = new boolean[6];
    private double customerProgress = 0;
    private double brewProgress = 0;

    public CafeGamePanel() {
        getStyleClass().add("cafe-game-panel");
        setSpacing(8);
        setPadding(new Insets(2, 0, 0, 0));
        setMinHeight(PANEL_HEIGHT);
        setPrefHeight(PANEL_HEIGHT);
        setMaxHeight(PANEL_HEIGHT);

        summaryLabel.getStyleClass().add("cafe-game-summary");
        levelLabel.getStyleClass().add("cafe-game-levels");
        summaryLabel.setWrapText(true);
        levelLabel.setWrapText(true);

        canvas.widthProperty().bind(widthProperty());
        canvas.setOnMouseClicked(event -> handleCanvasClick(event.getX(), event.getY(), Math.max(canvas.getWidth(), PANEL_WIDTH)));

        FlowPane buttonRow = new FlowPane();
        buttonRow.setHgap(8);
        buttonRow.setVgap(8);
        buttonRow.setPrefWrapLength(PANEL_WIDTH);
        buttonRow.getChildren().addAll(coffeeUpgradeButton, staffUpgradeButton, interiorUpgradeButton);

        coffeeUpgradeButton.getStyleClass().addAll("soft-button", "cafe-game-upgrade-button");
        staffUpgradeButton.getStyleClass().addAll("soft-button", "cafe-game-upgrade-button");
        interiorUpgradeButton.getStyleClass().addAll("soft-button", "cafe-game-upgrade-button");

        coffeeUpgradeButton.setOnAction(event -> upgradeCoffeeMachine());
        staffUpgradeButton.setOnAction(event -> upgradeStaffSpeed());
        interiorUpgradeButton.setOnAction(event -> buyInteriorUnlock());

        logListView.getStyleClass().add("cafe-game-log-list");
        logListView.setFocusTraversable(false);
        logListView.setMouseTransparent(true);
        logListView.setPrefHeight(64);
        logListView.setMinHeight(64);
        logListView.setMaxHeight(64);
        VBox.setVgrow(logListView, Priority.NEVER);

        getChildren().addAll(summaryLabel, levelLabel, canvas, buttonRow, logListView);

        gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateGame()));
        gameTimer.setCycleCount(Timeline.INDEFINITE);
        gameTimer.play();

        timeOwned[0] = true;
        floorOwned[0] = true;
        wallOwned[0] = true;
        updateInteriorSelections();
        addLog("Idle cafe opened. Coins ready: 30.");
        refreshUi();
    }

    public void stop() {
        gameTimer.stop();
    }

    private void updateGame() {
        growQueue();
        brewAndServe();
        refreshUi();
    }

    private void growQueue() {
        customerProgress += 0.7 + (interiorLevel * 0.12);
        int maxQueue = 4 + interiorLevel;

        while (customerProgress >= CUSTOMER_THRESHOLD) {
            customerProgress -= CUSTOMER_THRESHOLD;
            if (queue < maxQueue) {
                queue++;
            }
        }
    }

    private void brewAndServe() {
        if (queue <= 0) {
            brewProgress = 0;
            return;
        }

        brewProgress += 1.0 + ((staffSpeedLevel - 1) * 0.45);
        while (brewProgress >= ORDER_THRESHOLD && queue > 0) {
            brewProgress -= ORDER_THRESHOLD;
            queue--;
            batchProgress++;
            totalCustomersServed++;
            addLog("Drink served to table.");

            if (batchProgress >= 3) {
                int earned = batchCoinReward();
                coins += earned;
                totalRevenue += earned;
                batchProgress = 0;
                comboStreak++;
                addLog("Batch complete. +" + earned + " coins.");
            }
        }
    }

    private int batchCoinReward() {
        return (10 * coffeeMachineLevel) + ((staffSpeedLevel - 1) * 2) + ((interiorLevel - 1) * 4);
    }

    private void upgradeCoffeeMachine() {
        int cost = coffeeUpgradeCost();
        if (!spendCoins(cost, "Machine upgrade")) {
            return;
        }
        coffeeMachineLevel++;
        addLog("Machine Lv." + coffeeMachineLevel + " unlocked.");
        refreshUi();
    }

    private void upgradeStaffSpeed() {
        int cost = staffUpgradeCost();
        if (!spendCoins(cost, "Staff speed")) {
            return;
        }
        staffSpeedLevel++;
        addLog("Staff speed Lv." + staffSpeedLevel + " unlocked.");
        refreshUi();
    }

    private void buyInteriorUnlock() {
        if (interiorLevel - 1 >= INTERIOR_UNLOCKS.length) {
            addLog("All decor items purchased.");
            refreshUi();
            return;
        }

        InteriorUnlock unlock = INTERIOR_UNLOCKS[interiorLevel - 1];
        if (!spendCoins(unlock.cost(), unlock.name())) {
            return;
        }

        interiorLevel++;
        updateInteriorSelections();
        addLog(unlock.name() + " purchased.");
        refreshUi();
    }

    private void updateInteriorSelections() {
        int decorCount = interiorLevel - 1;
        selectedThemeIndex = Math.min(decorCount / 3, WINDOW_THEMES.length - 1);
        selectedFloorIndex = Math.min(decorCount / 2, FLOOR_STYLES.length - 1);
        selectedWallIndex = Math.min(decorCount / 2, WALL_STYLES.length - 1);
    }

    private boolean spendCoins(int cost, String actionName) {
        if (coins < cost) {
            addLog(actionName + " needs " + cost + " coins.");
            refreshUi();
            return false;
        }
        coins -= cost;
        return true;
    }

    private int coffeeUpgradeCost() {
        return 30 * coffeeMachineLevel;
    }

    private int staffUpgradeCost() {
        return 25 * staffSpeedLevel;
    }

    private int currentInteriorCost() {
        if (interiorLevel - 1 >= INTERIOR_UNLOCKS.length) {
            return -1;
        }
        return INTERIOR_UNLOCKS[interiorLevel - 1].cost();
    }

    private String nextInteriorName() {
        if (interiorLevel - 1 >= INTERIOR_UNLOCKS.length) {
            return "MAX";
        }
        return INTERIOR_UNLOCKS[interiorLevel - 1].name();
    }

    private void refreshUi() {
        summaryLabel.setText("Coins " + coins + "  |  Queue " + queue + "  |  Batch " + batchProgress + "/3");
        levelLabel.setText("Machine Lv." + coffeeMachineLevel
                + "  |  Staff Lv." + staffSpeedLevel
                + "  |  Interior Lv." + interiorLevel
                + "  |  Next " + shorten(nextInteriorName()));

        coffeeUpgradeButton.setText("Machine (" + coffeeUpgradeCost() + ")");
        staffUpgradeButton.setText("Staff Speed (" + staffUpgradeCost() + ")");
        interiorUpgradeButton.setText(currentInteriorCost() < 0
                ? "Interior MAX"
                : "Interior (" + currentInteriorCost() + ")");

        coffeeUpgradeButton.setDisable(coins < coffeeUpgradeCost());
        staffUpgradeButton.setDisable(coins < staffUpgradeCost());
        interiorUpgradeButton.setDisable(currentInteriorCost() < 0 || coins < currentInteriorCost());

        repaint();
    }

    private String shorten(String value) {
        return value.length() <= 10 ? value : value.substring(0, 10) + "...";
    }

    private void handleCanvasClick(double x, double y, double panelWidth) {
        int itemIndex = clickedItemIndex(panelWidth, x, y);
        if (itemIndex >= 0) {
            unlockItemObject(itemIndex);
            return;
        }
        int timeIndex = clickedTimeIndex(panelWidth, x, y);
        if (timeIndex >= 0) {
            unlockOrSelectTime(timeIndex);
            return;
        }
        SurfaceSelection surfaceSelection = clickedSurfaceIndex(panelWidth, x, y);
        if (surfaceSelection != null) {
            unlockOrSelectSurface(surfaceSelection);
        }
    }

    private void unlockItemObject(int index) {
        if (itemOwned[index]) {
            addLog(itemName(index) + " already purchased.");
            refreshUi();
            return;
        }
        if (!spendCoins(ITEM_OBJECT_COST, itemName(index))) {
            return;
        }
        itemOwned[index] = true;
        addLog(itemName(index) + " purchased.");
        refreshUi();
    }

    private void unlockOrSelectTime(int index) {
        if (!timeOwned[index]) {
            if (!spendCoins(TIME_OBJECT_COST, WINDOW_THEMES[index].label() + " theme")) {
                return;
            }
            timeOwned[index] = true;
            addLog(WINDOW_THEMES[index].label() + " theme unlocked.");
        }
        selectedThemeIndex = index;
        addLog("Theme set to " + WINDOW_THEMES[index].label() + ".");
        refreshUi();
    }

    private void unlockOrSelectSurface(SurfaceSelection selection) {
        if (selection.floor()) {
            if (!floorOwned[selection.index()]) {
                if (!spendCoins(SURFACE_OBJECT_COST, "Floor finish")) {
                    return;
                }
                floorOwned[selection.index()] = true;
                addLog("Floor finish unlocked.");
            }
            selectedFloorIndex = selection.index();
            addLog("Floor finish selected.");
        } else {
            if (!wallOwned[selection.index()]) {
                if (!spendCoins(SURFACE_OBJECT_COST, "Wallpaper")) {
                    return;
                }
                wallOwned[selection.index()] = true;
                addLog("Wallpaper unlocked.");
            }
            selectedWallIndex = selection.index();
            addLog("Wallpaper selected.");
        }
        refreshUi();
    }

    private void repaint() {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        double width = Math.max(canvas.getWidth(), PANEL_WIDTH);

        graphics.setFill(Color.rgb(246, 238, 228));
        graphics.fillRoundRect(0, 0, width, SCENE_HEIGHT, 20, 20);

        drawCafeScene(graphics, width);
        drawRosterColumn(graphics, width);
        drawInfoCards(graphics, width);
        drawIconPanel(graphics, width, SHOP_CARD_Y);
        drawWindowPanel(graphics, width, SHOP_CARD_Y);
        drawMaterialPanel(graphics, width, SHOP_CARD_Y);
    }

    private void drawCafeScene(GraphicsContext graphics, double width) {
        double sceneX = 10;
        double sceneY = 10;
        double sceneWidth = Math.max(178, width - 120);
        WindowTheme theme = WINDOW_THEMES[selectedThemeIndex];
        SurfacePalette floor = FLOOR_STYLES[selectedFloorIndex];
        SurfacePalette wall = WALL_STYLES[selectedWallIndex];

        graphics.setFill(wall.primary());
        graphics.fillRoundRect(sceneX, sceneY, sceneWidth, 138, 18, 18);

        for (int row = 0; row < 7; row++) {
            double plankY = sceneY + (row * 15);
            graphics.setStroke(wall.secondary());
            graphics.setLineWidth(1.4);
            graphics.strokeLine(sceneX, plankY, sceneX + sceneWidth, plankY);
        }

        graphics.setFill(floor.primary());
        graphics.fillRect(sceneX, sceneY + 98, sceneWidth, 40);
        for (int plank = 0; plank < 8; plank++) {
            graphics.setStroke(floor.secondary());
            graphics.setLineWidth(1.2);
            graphics.strokeLine(sceneX + (plank * 24), sceneY + 98, sceneX + (plank * 24), sceneY + 138);
        }
        for (int row = 0; row < 3; row++) {
            graphics.strokeLine(sceneX, sceneY + 112 + (row * 12), sceneX + sceneWidth, sceneY + 112 + (row * 12));
        }

        drawPendantLight(graphics, 26, 18);
        if (decorUnlocked(1)) {
            drawPendantLight(graphics, 84, 16);
        }
        if (decorUnlocked(6)) {
            drawPendantLight(graphics, 146, 18);
        }

        drawMenuBoard(graphics, sceneX + 40, 26);
        if (decorUnlocked(3)) {
            drawWallFrames(graphics, sceneX + 8, 42);
        }
        if (decorUnlocked(2)) {
            drawShelfCluster(graphics, sceneX + 86, 28);
        }
        drawWindow(graphics, sceneX + sceneWidth - 46, 36, 42, 50, theme);
        drawCounterArea(graphics, 12, 76, sceneWidth - 24);
        drawCafeFurniture(graphics);
        drawActiveGuests(graphics);
    }

    private boolean decorUnlocked(int unlockNumber) {
        return (interiorLevel - 1) >= unlockNumber;
    }

    private void drawPendantLight(GraphicsContext graphics, double x, double y) {
        graphics.setStroke(Color.rgb(46, 34, 28));
        graphics.setLineWidth(2);
        graphics.strokeLine(x + 7, 0, x + 7, y + 6);
        graphics.setFill(Color.rgb(69, 48, 35));
        graphics.fillArc(x, y, 14, 9, 0, 180, ArcType.ROUND);
        graphics.setFill(Color.rgb(248, 208, 129, 0.82));
        graphics.fillOval(x + 2.5, y + 8, 9, 9);
    }

    private void drawMenuBoard(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(48, 38, 29));
        graphics.fillRoundRect(x, y, 42, 24, 6, 6);
        graphics.setStroke(Color.rgb(145, 110, 72));
        graphics.setLineWidth(2);
        graphics.strokeRoundRect(x, y, 42, 24, 6, 6);
        graphics.setFill(Color.rgb(226, 198, 150));
        graphics.setFont(SMALL_BOLD_FONT);
        graphics.fillText("MENU", x + 10, y + 9);
        graphics.setFont(SMALL_FONT);
        graphics.fillText("ESP", x + 4, y + 16);
        graphics.fillText("LAT", x + 22, y + 16);
        graphics.fillText("TEA", x + 4, y + 22);
        graphics.fillText("DESS", x + 20, y + 22);
    }

    private void drawWallFrames(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(188, 162, 118));
        graphics.fillRect(x, y, 10, 14);
        graphics.setFill(Color.rgb(214, 233, 198));
        graphics.fillRect(x + 2, y + 2, 6, 10);

        graphics.setFill(Color.rgb(152, 118, 86));
        graphics.fillRect(x + 108, y + 42, 14, 10);
        graphics.setFill(Color.rgb(203, 219, 181));
        graphics.fillRect(x + 110, y + 44, 10, 6);
    }

    private void drawShelfCluster(GraphicsContext graphics, double x, double y) {
        graphics.setStroke(Color.rgb(90, 64, 43));
        graphics.setLineWidth(3);
        graphics.strokeLine(x, y, x + 40, y);
        graphics.strokeLine(x + 4, y + 18, x + 44, y + 18);

        for (int index = 0; index < 4; index++) {
            graphics.setFill(Color.rgb(203, 191, 170));
            graphics.fillRoundRect(x + 4 + (index * 9), y + 20, 6, 5, 2, 2);
        }
        drawPlantPot(graphics, x + 10, y - 10, 6, 9, Color.rgb(93, 137, 73));
        drawPlantPot(graphics, x + 24, y - 12, 6, 11, Color.rgb(120, 151, 73));
        drawPlantPot(graphics, x + 36, y - 8, 6, 8, Color.rgb(80, 121, 74));
        drawPlantPot(graphics, x + 40, y + 4, 5, 9, Color.rgb(74, 141, 94));
    }

    private void drawWindow(GraphicsContext graphics, double x, double y, double width, double height, WindowTheme theme) {
        graphics.setFill(Color.rgb(60, 44, 36));
        graphics.fillRect(x, y, width, height);

        graphics.setFill(theme.topColor());
        graphics.fillRect(x + 3, y + 3, width - 6, (height - 6) / 2);
        graphics.setFill(theme.bottomColor());
        graphics.fillRect(x + 3, y + 3 + ((height - 6) / 2), width - 6, (height - 6) / 2);

        if (theme.night()) {
            graphics.setFill(theme.accentColor());
            graphics.fillOval(x + 24, y + 7, 8, 8);
            graphics.setFill(theme.bottomColor());
            graphics.fillOval(x + 27, y + 7, 8, 8);
        } else {
            graphics.setFill(theme.accentColor());
            graphics.fillOval(x + 24, y + 8, 8, 8);
        }

        graphics.setFill(Color.rgb(255, 214, 135, 0.9));
        for (int building = 0; building < 4; building++) {
            double buildingX = x + 5 + (building * 8);
            double buildingHeight = theme.night() ? 8 + (building * 3) : 5 + (building * 2);
            graphics.fillRect(buildingX, y + height - 10 - buildingHeight, 5, buildingHeight);
        }

        graphics.setStroke(Color.rgb(73, 54, 43));
        graphics.setLineWidth(2);
        graphics.strokeLine(x + (width / 2), y, x + (width / 2), y + height);
        graphics.strokeLine(x, y + (height / 2), x + width, y + (height / 2));
    }

    private void drawCounterArea(GraphicsContext graphics, double x, double y, double width) {
        graphics.setFill(Color.rgb(113, 71, 47));
        graphics.fillRoundRect(x, y, width, 28, 12, 12);
        graphics.setFill(Color.rgb(90, 56, 36));
        graphics.fillRect(x, y + 17, width, 11);

        drawEspressoMachine(graphics, x + 6, y - 1);
        drawGrinder(graphics, x + 43, y + 1);
        drawDripMachine(graphics, x + 114, y + 2);
        if (decorUnlocked(5)) {
            drawDisplayCase(graphics, x + 134, y + 1);
        }
        drawRegister(graphics, x + 102, y + 11);
        drawCupRow(graphics, x + 10, y - 8);
        drawCupRow(graphics, x + 128, y - 7);
        drawBarista(graphics, x + 78, y - 2, STAFF_PALETTES[0], true);
        drawPlantPot(graphics, x - 2, y + 16, 8, 14, Color.rgb(79, 134, 84));
    }

    private void drawEspressoMachine(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(74, 73, 73));
        graphics.fillRoundRect(x, y, 34, 16, 4, 4);
        graphics.setFill(Color.rgb(201, 194, 185));
        graphics.fillRoundRect(x + 3, y + 2, 8, 5, 3, 3);
        graphics.fillRoundRect(x + 13, y + 2, 8, 5, 3, 3);
        graphics.fillRoundRect(x + 23, y + 2, 8, 5, 3, 3);
        graphics.setStroke(Color.rgb(221, 212, 199));
        graphics.setLineWidth(1.5);
        graphics.strokeLine(x + 10, y + 8, x + 10, y + 13);
        graphics.strokeLine(x + 18, y + 8, x + 18, y + 13);
        graphics.strokeLine(x + 26, y + 8, x + 26, y + 13);
    }

    private void drawGrinder(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(55, 45, 37));
        graphics.fillRoundRect(x + 3, y + 5, 10, 12, 3, 3);
        graphics.setFill(Color.rgb(196, 159, 108));
        graphics.fillOval(x + 2, y, 12, 6);
    }

    private void drawDripMachine(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(44, 43, 44));
        graphics.fillRoundRect(x, y, 12, 16, 4, 4);
        graphics.setFill(Color.rgb(130, 90, 53));
        graphics.fillOval(x + 3, y + 8, 6, 4);
    }

    private void drawDisplayCase(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(52, 55, 57));
        graphics.fillRoundRect(x, y, 18, 22, 4, 4);
        graphics.setFill(Color.rgb(233, 216, 181));
        graphics.fillOval(x + 3, y + 5, 5, 3);
        graphics.fillOval(x + 10, y + 5, 5, 3);
        graphics.fillOval(x + 4, y + 12, 5, 3);
        graphics.fillOval(x + 10, y + 12, 5, 3);
    }

    private void drawRegister(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(58, 66, 74));
        graphics.fillRoundRect(x, y, 11, 8, 2, 2);
        graphics.setFill(Color.rgb(148, 205, 233));
        graphics.fillRect(x + 2, y + 1, 6, 3);
    }

    private void drawCupRow(GraphicsContext graphics, double x, double y) {
        for (int index = 0; index < 3; index++) {
            drawCupIcon(graphics, x + (index * 6), y, 1.0);
        }
    }

    private void drawCafeFurniture(GraphicsContext graphics) {
        drawRoundTable(graphics, 34, 116);
        drawChair(graphics, 22, 114);
        drawChair(graphics, 58, 114);
        drawCupIcon(graphics, 40, 113, 1.0);

        if (decorUnlocked(4)) {
            drawCabinet(graphics, 98, 112);
        }
        if (decorUnlocked(2)) {
            drawWallShelf(graphics, 110, 110);
        }
        if (decorUnlocked(5)) {
            drawRadio(graphics, 150, 116);
        }
        if (decorUnlocked(6)) {
            drawSofa(graphics, 142, 122, 18, 10);
        }
        if (decorUnlocked(2)) {
            drawPlantPot(graphics, 160, 114, 7, 13, Color.rgb(88, 135, 83));
        }
    }

    private void drawRoundTable(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(143, 92, 53));
        graphics.fillOval(x, y, 22, 8);
        graphics.fillRect(x + 10, y + 6, 2, 10);
        graphics.fillOval(x + 6, y + 15, 10, 3);
    }

    private void drawChair(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(73, 58, 46));
        graphics.fillRoundRect(x, y, 8, 10, 3, 3);
        graphics.fillRect(x + 1, y + 9, 1, 6);
        graphics.fillRect(x + 6, y + 9, 1, 6);
    }

    private void drawCabinet(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(112, 76, 49));
        graphics.fillRoundRect(x, y, 18, 10, 3, 3);
        graphics.fillRect(x, y + 8, 18, 11);
        graphics.setFill(Color.rgb(190, 155, 110));
        graphics.fillOval(x + 4, y + 12, 1.5, 1.5);
        graphics.fillOval(x + 12, y + 12, 1.5, 1.5);
    }

    private void drawWallShelf(GraphicsContext graphics, double x, double y) {
        graphics.setStroke(Color.rgb(121, 86, 56));
        graphics.setLineWidth(2);
        graphics.strokeLine(x, y, x + 20, y);
        graphics.setFill(Color.rgb(108, 142, 87));
        graphics.fillOval(x + 2, y - 8, 7, 7);
        graphics.fillOval(x + 12, y - 7, 6, 6);
        graphics.setFill(Color.rgb(177, 149, 109));
        graphics.fillRect(x + 2, y + 2, 4, 6);
        graphics.fillRect(x + 7, y + 1, 3, 7);
        graphics.fillRect(x + 11, y + 3, 4, 5);
    }

    private void drawRadio(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(74, 62, 50));
        graphics.fillRoundRect(x, y, 14, 8, 3, 3);
        graphics.setStroke(Color.rgb(201, 183, 154));
        graphics.setLineWidth(1.2);
        graphics.strokeRect(x + 2, y + 2, 4, 3);
        graphics.strokeLine(x + 10, y, x + 12, y - 5);
    }

    private void drawSofa(GraphicsContext graphics, double x, double y, double width, double height) {
        graphics.setFill(Color.rgb(65, 90, 104));
        graphics.fillRoundRect(x, y, width, height, 4, 4);
        graphics.fillRect(x - 1, y + 4, 2, height - 2);
        graphics.fillRect(x + width - 1, y + 4, 2, height - 2);
    }

    private void drawPlantPot(GraphicsContext graphics, double x, double y, double width, double height, Color leafColor) {
        graphics.setFill(leafColor);
        graphics.fillOval(x, y, width, height - 4);
        graphics.setFill(Color.rgb(126, 90, 55));
        graphics.fillRect(x + 1, y + height - 5, width - 2, 4);
    }

    private void drawActiveGuests(GraphicsContext graphics) {
        if (queue > 0) {
            drawCustomer(graphics, 31, 100, CUSTOMER_PALETTES[0], 0.92);
            drawSpeechBubble(graphics, 24, 72, BUBBLE_LABELS[0]);
        }
        if (queue > 2) {
            drawCustomer(graphics, 118, 100, CUSTOMER_PALETTES[3], 0.82);
            drawSpeechBubble(graphics, 110, 72, BUBBLE_LABELS[2]);
        }
    }

    private void drawCustomer(GraphicsContext graphics, double x, double y, AvatarPalette palette, double scale) {
        drawAvatar(graphics, x, y, palette, scale, false);
    }

    private void drawBarista(GraphicsContext graphics, double x, double y, AvatarPalette palette, boolean active) {
        drawAvatar(graphics, x, y, palette, active ? 1.02 : 0.8, true);
    }

    private void drawAvatar(GraphicsContext graphics, double x, double y, AvatarPalette palette, double scale, boolean staff) {
        double head = 9 * scale;
        double bodyWidth = 10 * scale;
        double bodyHeight = 12 * scale;

        graphics.setFill(palette.hairColor());
        graphics.fillOval(x + (2 * scale), y, head, head);
        if (palette.accessory() == Accessory.BUN) {
            graphics.fillOval(x + (7 * scale), y - (2 * scale), 4 * scale, 4 * scale);
        }
        if (palette.accessory() == Accessory.CAP) {
            graphics.setFill(Color.rgb(42, 62, 91));
            graphics.fillRoundRect(x + (1 * scale), y + (1 * scale), head, 4 * scale, 4 * scale, 4 * scale);
        }

        graphics.setFill(Color.rgb(244, 219, 193));
        graphics.fillOval(x + (3 * scale), y + (3 * scale), 6 * scale, 6 * scale);

        graphics.setFill(palette.outfitColor());
        graphics.fillRoundRect(x + (2 * scale), y + (8 * scale), bodyWidth, bodyHeight, 4 * scale, 4 * scale);
        graphics.setFill(palette.pantsColor());
        graphics.fillRect(x + (3 * scale), y + (17 * scale), 3 * scale, 5 * scale);
        graphics.fillRect(x + (8 * scale), y + (17 * scale), 3 * scale, 5 * scale);

        graphics.setFill(Color.rgb(63, 53, 45));
        graphics.fillRect(x + (3 * scale), y + (21 * scale), 3 * scale, 1.8 * scale);
        graphics.fillRect(x + (8 * scale), y + (21 * scale), 3 * scale, 1.8 * scale);

        if (staff || palette.accessory() == Accessory.APRON || palette.accessory() == Accessory.PONYTAIL) {
            graphics.setFill(Color.rgb(56, 50, 48));
            graphics.fillRect(x + (4 * scale), y + (9 * scale), 5 * scale, 9 * scale);
        }
        if (palette.accessory() == Accessory.GLASSES) {
            graphics.setStroke(Color.rgb(54, 54, 55));
            graphics.setLineWidth(Math.max(1, scale));
            graphics.strokeRect(x + (4 * scale), y + (5 * scale), 2 * scale, 2 * scale);
            graphics.strokeRect(x + (7 * scale), y + (5 * scale), 2 * scale, 2 * scale);
        }
        if (palette.accessory() == Accessory.PONYTAIL) {
            graphics.setFill(palette.hairColor());
            graphics.fillOval(x + (9 * scale), y + (2 * scale), 4 * scale, 4 * scale);
        }
    }

    private void drawSpeechBubble(GraphicsContext graphics, double x, double y, String label) {
        graphics.setFill(Color.rgb(255, 250, 244));
        graphics.fillRoundRect(x, y, 28, 12, 8, 8);
        graphics.fillPolygon(new double[] {x + 8, x + 11, x + 7}, new double[] {y + 12, y + 15, y + 14}, 3);
        graphics.setStroke(Color.rgb(92, 74, 61));
        graphics.setLineWidth(1.1);
        graphics.strokeRoundRect(x, y, 28, 12, 8, 8);
        graphics.setFill(Color.rgb(92, 74, 61));
        graphics.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 7.5));
        graphics.fillText(label, x + 4, y + 8);
    }

    private void drawRosterColumn(GraphicsContext graphics, double width) {
        double panelX = width - 90;
        double panelY = 10;
        double panelWidth = 80;
        double panelHeight = 138;

        graphics.setFill(Color.rgb(255, 249, 241));
        graphics.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);
        graphics.setStroke(Color.rgb(224, 208, 188));
        graphics.setLineWidth(1.2);
        graphics.strokeRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);

        graphics.setFill(Color.rgb(93, 66, 48));
        graphics.setFont(LABEL_FONT);
        graphics.fillText("GUESTS", panelX + 6, panelY + 11);
        drawGuestRoster(graphics, panelX + 6, panelY + 16);

        graphics.fillText("STAFF", panelX + 6, panelY + 67);
        drawStaffRoster(graphics, panelX + 6, panelY + 72);

        graphics.fillText("MOOD", panelX + 6, panelY + 112);
        drawBubbleRoster(graphics, panelX + 4, panelY + 116);
    }

    private void drawGuestRoster(GraphicsContext graphics, double startX, double startY) {
        for (int index = 0; index < CUSTOMER_PALETTES.length; index++) {
            double x = startX + ((index % 4) * 17);
            double y = startY + ((index / 4) * 22);
            drawAvatar(graphics, x, y, CUSTOMER_PALETTES[index], 0.54, false);
            if (index == 4) {
                graphics.setStroke(Color.rgb(205, 153, 101));
                graphics.setLineWidth(1.2);
                graphics.strokeRoundRect(x - 1, y - 1, 14, 15, 5, 5);
            }
        }
    }

    private void drawStaffRoster(GraphicsContext graphics, double startX, double startY) {
        for (int index = 0; index < STAFF_PALETTES.length; index++) {
            double x = startX + (index * 21);
            drawAvatar(graphics, x, startY, STAFF_PALETTES[index], 0.6, true);
            if (index == 1) {
                graphics.setStroke(Color.rgb(205, 153, 101));
                graphics.setLineWidth(1.2);
                graphics.strokeRoundRect(x - 1, startY - 1, 15, 16, 5, 5);
            }
        }
    }

    private void drawBubbleRoster(GraphicsContext graphics, double startX, double startY) {
        for (int index = 0; index < BUBBLE_LABELS.length; index++) {
            double x = startX + (index * 18);
            drawSpeechBubble(graphics, x, startY, BUBBLE_LABELS[index]);
        }
    }

    private void drawInfoCards(GraphicsContext graphics, double width) {
        double cardWidth = (width - 36) / 3;
        double secondX = 18 + cardWidth;
        double thirdX = 26 + (cardWidth * 2);
        drawUiCard(graphics, 10, 156, cardWidth, 45, "CAFE",
                "Lv." + interiorLevel,
                "Coins " + coins,
                "Theme " + WINDOW_THEMES[selectedThemeIndex].label());
        drawUiCard(graphics, secondX, 156, cardWidth, 45, "UPGRADE",
                "Machine " + coffeeMachineLevel,
                "Staff " + staffSpeedLevel,
                "Interior " + interiorLevel);
        drawUiCard(graphics, thirdX, 156, cardWidth, 45, "INFO",
                "Rev " + totalRevenue,
                "Guest " + totalCustomersServed,
                "Combo " + comboStreak);
    }

    private void drawUiCard(GraphicsContext graphics, double x, double y, double width, double height,
                            String title, String line1, String line2, String line3) {
        graphics.setFill(Color.rgb(92, 60, 38));
        graphics.fillRoundRect(x, y, width, height, 10, 10);
        graphics.setFill(Color.rgb(140, 98, 67));
        graphics.fillRect(x, y + 11, width, 1.5);
        graphics.setFill(Color.rgb(244, 224, 192));
        graphics.setFont(LABEL_FONT);
        graphics.fillText(title, x + 8, y + 10);
        graphics.setFont(SMALL_FONT);
        graphics.fillText(line1, x + 8, y + 22);
        graphics.fillText(line2, x + 8, y + 31);
        graphics.fillText(line3, x + 8, y + 40);
    }

    private void drawIconPanel(GraphicsContext graphics, double panelWidth, double y) {
        ShopCard card = itemCard(panelWidth);
        double x = card.x();
        double width = card.width();
        double height = card.height();
        drawCatalogCard(graphics, x, y, width, height, "ITEMS");
        for (int index = 0; index < itemOwned.length; index++) {
            ShopSlot slot = itemSlot(panelWidth, index);
            drawItemSlot(graphics, slot, index);
        }
    }

    private void drawWindowPanel(GraphicsContext graphics, double panelWidth, double y) {
        ShopCard card = timeCard(panelWidth);
        double x = card.x();
        double width = card.width();
        double height = card.height();
        drawCatalogCard(graphics, x, y, width, height, "TIME");
        for (int index = 0; index < WINDOW_THEMES.length; index++) {
            ShopSlot slot = timeSlot(panelWidth, index);
            double slotX = slot.x();
            WindowTheme theme = WINDOW_THEMES[index];
            graphics.setFill(Color.rgb(82, 58, 44));
            graphics.fillRect(slotX, slot.y(), slot.width(), slot.height());
            graphics.setFill(theme.topColor());
            graphics.fillRect(slotX + 2, slot.y() + 2, 12, 5);
            graphics.setFill(theme.bottomColor());
            graphics.fillRect(slotX + 2, slot.y() + 7, 12, 7);
            if (theme.night()) {
                graphics.setFill(theme.accentColor());
                graphics.fillOval(slotX + 9, slot.y() + 3, 3, 3);
            }
            graphics.setFill(timeOwned[index] ? Color.rgb(243, 226, 196) : Color.rgb(214, 197, 173));
            graphics.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 6.5));
            graphics.fillText(theme.label(), slotX + 1, y + 34);
            if (!timeOwned[index]) {
                drawMiniLock(graphics, slot, TIME_OBJECT_COST);
            } else if (index == selectedThemeIndex) {
                graphics.setStroke(Color.rgb(205, 153, 101));
                graphics.setLineWidth(1.0);
                graphics.strokeRoundRect(slotX - 1, y + 8, 18, 18, 4, 4);
            }
        }
    }

    private void drawMaterialPanel(GraphicsContext graphics, double panelWidth, double y) {
        ShopCard card = surfaceCard(panelWidth);
        double x = card.x();
        double width = card.width();
        double height = card.height();
        drawCatalogCard(graphics, x, y, width, height, "SURFACE");
        for (int index = 0; index < FLOOR_STYLES.length; index++) {
            ShopSlot slot = floorSlot(panelWidth, index);
            drawSwatch(graphics, slot.x(), slot.y(), slot.width(), slot.height(),
                    FLOOR_STYLES[index], floorOwned[index] && index == selectedFloorIndex);
            if (!floorOwned[index]) {
                drawMiniLock(graphics, slot, SURFACE_OBJECT_COST);
            }
        }
        for (int index = 0; index < WALL_STYLES.length; index++) {
            ShopSlot slot = wallSlot(panelWidth, index);
            drawSwatch(graphics, slot.x(), slot.y(), slot.width(), slot.height(),
                    WALL_STYLES[index], wallOwned[index] && index == selectedWallIndex);
            if (!wallOwned[index]) {
                drawMiniLock(graphics, slot, SURFACE_OBJECT_COST);
            }
        }
    }

    private void drawMiniLock(GraphicsContext graphics, ShopSlot slot, int cost) {
        graphics.setFill(Color.rgb(255, 248, 240, 0.88));
        graphics.fillRoundRect(slot.x() - 1, slot.y() - 1, slot.width() + 2, slot.height() + 2, 5, 5);
        graphics.setStroke(Color.rgb(188, 167, 142));
        graphics.setLineWidth(0.8);
        graphics.strokeRoundRect(slot.x() - 1, slot.y() - 1, slot.width() + 2, slot.height() + 2, 5, 5);
        graphics.setFill(Color.rgb(102, 81, 61));
        graphics.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 7));
        graphics.fillText("L", slot.x() + (slot.width() / 2) - 2, slot.y() + 7);
        graphics.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 6));
        graphics.fillText(String.valueOf(cost), slot.x() - 1, slot.y() + slot.height() + 6);
    }

    private ShopCard itemCard(double panelWidth) {
        double cardWidth = (panelWidth - 36) / 3;
        return new ShopCard(10, SHOP_CARD_Y, cardWidth, SHOP_CARD_HEIGHT);
    }

    private ShopCard timeCard(double panelWidth) {
        double cardWidth = (panelWidth - 36) / 3;
        return new ShopCard(18 + cardWidth, SHOP_CARD_Y, cardWidth, SHOP_CARD_HEIGHT);
    }

    private ShopCard surfaceCard(double panelWidth) {
        double cardWidth = (panelWidth - 36) / 3;
        return new ShopCard(26 + (cardWidth * 2), SHOP_CARD_Y, cardWidth, SHOP_CARD_HEIGHT);
    }

    private ShopSlot itemSlot(double panelWidth, int index) {
        ShopCard card = itemCard(panelWidth);
        return new ShopSlot(card.x() + 8 + ((index % 4) * 18), card.y() + 8 + ((index / 4) * 18), SHOP_ITEM_SIZE, SHOP_ITEM_SIZE);
    }

    private ShopSlot timeSlot(double panelWidth, int index) {
        ShopCard card = timeCard(panelWidth);
        return new ShopSlot(card.x() + 6 + (index * 19), card.y() + 9, SHOP_ITEM_SIZE, SHOP_ITEM_SIZE);
    }

    private ShopSlot floorSlot(double panelWidth, int index) {
        ShopCard card = surfaceCard(panelWidth);
        return new ShopSlot(card.x() + 5 + (index * 14.6), card.y() + 10, 12, 8);
    }

    private ShopSlot wallSlot(double panelWidth, int index) {
        ShopCard card = surfaceCard(panelWidth);
        return new ShopSlot(card.x() + 4 + (index * 12.3), card.y() + 24, 10, 8);
    }

    private int clickedItemIndex(double panelWidth, double x, double y) {
        for (int index = 0; index < itemOwned.length; index++) {
            if (itemSlot(panelWidth, index).contains(x, y)) {
                return index;
            }
        }
        return -1;
    }

    private int clickedTimeIndex(double panelWidth, double x, double y) {
        for (int index = 0; index < WINDOW_THEMES.length; index++) {
            if (timeSlot(panelWidth, index).contains(x, y)) {
                return index;
            }
        }
        return -1;
    }

    private SurfaceSelection clickedSurfaceIndex(double panelWidth, double x, double y) {
        for (int index = 0; index < FLOOR_STYLES.length; index++) {
            if (floorSlot(panelWidth, index).contains(x, y)) {
                return new SurfaceSelection(true, index);
            }
        }
        for (int index = 0; index < WALL_STYLES.length; index++) {
            if (wallSlot(panelWidth, index).contains(x, y)) {
                return new SurfaceSelection(false, index);
            }
        }
        return null;
    }

    private void drawItemSlot(GraphicsContext graphics, ShopSlot slot, int index) {
        double x = slot.x();
        double y = slot.y();
        switch (index) {
            case 0 -> drawCupIcon(graphics, x + 3, y + 4, 1.0);
            case 1 -> drawCookie(graphics, x + 3, y + 3);
            case 2 -> drawCake(graphics, x + 3, y + 3);
            case 3 -> drawMuffin(graphics, x + 3, y + 4);
            case 4 -> drawBook(graphics, x + 4, y + 2);
            case 5 -> drawRecord(graphics, x + 3, y + 3);
            case 6 -> drawPlantPot(graphics, x + 4, y + 2, 8, 12, Color.rgb(86, 133, 83));
            case 7 -> drawSofa(graphics, x + 2, y + 5, 12, 7);
            default -> {
            }
        }
        if (!itemOwned[index]) {
            drawMiniLock(graphics, slot, ITEM_OBJECT_COST);
        }
    }

    private String itemName(int index) {
        return switch (index) {
            case 0 -> "Coffee Cup";
            case 1 -> "Cookie";
            case 2 -> "Cake Slice";
            case 3 -> "Muffin";
            case 4 -> "Book";
            case 5 -> "Record";
            case 6 -> "Plant Pot";
            case 7 -> "Sofa";
            default -> "Item";
        };
    }

    private void drawCatalogCard(GraphicsContext graphics, double x, double y, double width, double height, String title) {
        graphics.setFill(Color.rgb(255, 250, 243));
        graphics.fillRoundRect(x, y, width, height, 10, 10);
        graphics.setStroke(Color.rgb(224, 208, 188));
        graphics.setLineWidth(1.1);
        graphics.strokeRoundRect(x, y, width, height, 10, 10);
        graphics.setFill(Color.rgb(92, 66, 48));
        graphics.setFont(Font.font("Malgun Gothic", FontWeight.BOLD, 8.5));
        graphics.fillText(title, x + 6, y + 8);
    }

    private void drawSwatch(GraphicsContext graphics, double x, double y, double width, double height,
                            SurfacePalette palette, boolean active) {
        graphics.setFill(palette.primary());
        graphics.fillRect(x, y, width, height);
        graphics.setStroke(palette.secondary());
        graphics.setLineWidth(1);
        graphics.strokeRect(x, y, width, height);
        if (active) {
            graphics.setStroke(Color.rgb(205, 153, 101));
            graphics.setLineWidth(1.2);
            graphics.strokeRoundRect(x - 1, y - 1, width + 2, height + 2, 3, 3);
        }
    }

    private void drawCupIcon(GraphicsContext graphics, double x, double y, double scale) {
        graphics.setFill(Color.rgb(245, 239, 232));
        graphics.fillRoundRect(x, y, 7 * scale, 5 * scale, 2, 2);
        graphics.setStroke(Color.rgb(117, 86, 60));
        graphics.setLineWidth(1);
        graphics.strokeArc(x + 5 * scale, y + 1 * scale, 3 * scale, 3 * scale, 300, 240, ArcType.OPEN);
        graphics.setStroke(Color.rgb(144, 97, 57));
        graphics.strokeLine(x, y + 5.5 * scale, x + 8 * scale, y + 5.5 * scale);
    }

    private void drawCookie(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(193, 141, 84));
        graphics.fillOval(x, y, 10, 10);
        graphics.setFill(Color.rgb(93, 61, 39));
        graphics.fillOval(x + 2, y + 2, 1.5, 1.5);
        graphics.fillOval(x + 5, y + 4, 1.5, 1.5);
        graphics.fillOval(x + 6.5, y + 1.5, 1.5, 1.5);
    }

    private void drawCake(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(235, 206, 156));
        graphics.fillPolygon(new double[] {x, x + 10, x + 2}, new double[] {y + 9, y + 9, y}, 3);
        graphics.setFill(Color.rgb(255, 244, 230));
        graphics.fillPolygon(new double[] {x + 1, x + 8, x + 2}, new double[] {y + 7, y + 7, y + 2}, 3);
        graphics.setFill(Color.rgb(209, 74, 75));
        graphics.fillOval(x + 4, y - 1, 3, 3);
    }

    private void drawMuffin(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(95, 61, 40));
        graphics.fillOval(x, y, 10, 8);
        graphics.setFill(Color.rgb(137, 98, 64));
        graphics.fillRect(x + 1, y + 6, 8, 4);
    }

    private void drawBook(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(104, 74, 50));
        graphics.fillRoundRect(x, y, 9, 11, 2, 2);
        graphics.setStroke(Color.rgb(224, 205, 165));
        graphics.setLineWidth(1);
        graphics.strokeLine(x + 2, y + 2, x + 7, y + 2);
    }

    private void drawRecord(GraphicsContext graphics, double x, double y) {
        graphics.setFill(Color.rgb(34, 34, 37));
        graphics.fillOval(x, y, 10, 10);
        graphics.setFill(Color.rgb(206, 122, 66));
        graphics.fillOval(x + 3.5, y + 3.5, 3, 3);
    }

    private void addLog(String message) {
        logs.add(0, message);
        if (logs.size() > MAX_LOGS) {
            logs.remove(MAX_LOGS, logs.size());
        }
    }

    private record AvatarPalette(Color hairColor, Color outfitColor, Color pantsColor, Accessory accessory) { }

    private record WindowTheme(String label, Color topColor, Color bottomColor, Color accentColor, boolean night) { }

    private record SurfacePalette(Color primary, Color secondary) { }

    private record InteriorUnlock(String name, int cost) { }

    private record ShopCard(double x, double y, double width, double height) {
        private boolean contains(double pointX, double pointY) {
            return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
        }
    }

    private record ShopSlot(double x, double y, double width, double height) {
        private boolean contains(double pointX, double pointY) {
            return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
        }
    }

    private record SurfaceSelection(boolean floor, int index) { }

    private enum Accessory {
        NONE,
        GLASSES,
        CAP,
        BUN,
        APRON,
        PONYTAIL
    }
}
