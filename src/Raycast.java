import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

public class Raycast extends Canvas implements KeyListener, Runnable {
    private static final int WIDTH = 960+16;
    private static final int HEIGHT = 640+45;
    private final boolean[] keys;
    private double playerX, playerY, playerDeltaX, playerDeltaY, playerAngle;
    private static final double PLAYER_MOVE_SPEED = 3;
    private static final double PLAYER_TURN_SPEED = 2.5;
    private static final double ENEMY_MOVE_SPEED = 1;
    private static final int FRAMES_PER_SECOND = 60;
    private static final int PIXEL_SIZE = 8;
    private BufferedImage back;
    private final int[] skyTexture;
    private final int[] tilesTexture;
    private final int[] spritesTexture;
    private final int[] winTexture;
    private final int[] loseTexture;
    private final int[] titleTexture;
    private final Sprite[] sprites;
    private final int[] rayDepth;
    private int gameState=0;
    private double timer=0;
    private double fade=0;
    private static final int[][] MAP_WALLS = {
            {3, 3, 3, 3, 3, 3, 3, 3},
            {3, 0, 0, 2, 0, 0, 0, 3},
            {3, 0, 0, 3, 0, 3, 0, 3},
            {3, 3, 2, 3, 0, 0, 0, 3},
            {3, 0, 0, 0, 0, 0, 0, 3},
            {3, 0, 0, 0, 0, 3, 0, 3},
            {3, 0, 0, 0, 0, 0, 0, 3},
            {3, 3, 3, 3, 3, 3, 3, 3}};
    private static final int[][] MAP_FLOOR = {
            {4, 4, 4, 4, 4, 4, 4, 4},
            {4, 4, 4, 4, 4, 4, 4, 4},
            {4, 4, 4, 4, 4, 4, 4, 4},
            {4, 4, 4, 4, 4, 4, 4, 4},
            {4, 4, 4, 4, 4, 4, 4, 4},
            {4, 4, 4, 4, 4, 4, 4, 4},
            {4, 4, 4, 4, 4, 4, 4, 4},
            {4, 4, 4, 4, 4, 4, 4, 4}};
    private static final int[][] MAP_CEILING = {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0}};
    public Raycast() {
        keys = new boolean[5];
        rayDepth = new int[120];
        sprites = new Sprite[1];
        init();

        setBackground(new Color(70, 70, 70));
        this.addKeyListener(this);
        new Thread(this).start();
        setVisible(true);

        //add textures
        ArrayList<int[]> textures = new ArrayList<>();
        String folderPath = "Textures";
        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            assert files != null;
            for (File file : files) {
                if (file.isFile()) {
                    try {
                        textures.add(txtToTile(file.getAbsolutePath()));
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        skyTexture = textures.get(0);
        tilesTexture = textures.get(1);
        spritesTexture = textures.get(2);
        winTexture = textures.get(3);
        loseTexture = textures.get(4);
        titleTexture = textures.get(5);
    }
    private void init() {
        playerX = 1.5*64;
        playerY = 6.5*64;
        playerAngle = 45;
        playerDeltaX = Math.cos(degreeToRadian(playerAngle));
        playerDeltaY = -Math.sin(degreeToRadian(playerAngle));

        sprites[0] = new Sprite(1, true, 0, 1.5*64, 4.5*64, 20);
        //sprites[1] = new Sprite(3, true, 1, 6.5*64, 3.5*64, 20);

        MAP_WALLS[3][2] = 2;
        MAP_WALLS[1][3] = 2;
    }
    private int[] txtToTile(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        int length = Integer.parseInt(reader.readLine().trim());
        int[] array = new int[length*3];
        for (int i = 0; i < length; i++) {
            String[] rgb = reader.readLine().trim().split(" ");
            for (int j = 0; j < rgb.length; j++) {
                array[3*i + j] = Integer.parseInt(rgb[j]);
            }
        }
        reader.close();
        return array;
    }

    @Override
    public void update(Graphics g) {
        if(gameState==0){
            init();
            timer=0;
            fade=0;
            gameState=1;
        }
        if(gameState==1){
            drawGame(g);
            timer += 1000./FRAMES_PER_SECOND;

            if(fade<1) {fade += 0.018;}
            if (fade>1) {fade=1;}

            if(timer > 1000) {
                timer=0;
                gameState=2;
            }
        }
        if(gameState==2) {
            drawGame(g);
            fade=1;
        }
        if(gameState==3){
            //win screen
            drawScreen((Graphics2D) g, 2);
            timer += 1000./FRAMES_PER_SECOND;
            if(timer > 2 * 1000) {
                fade=0;
                timer=0;
                gameState=0;
            }
        }
        if(gameState==4){
            //lose screen
            drawScreen((Graphics2D) g, 3);
            timer += 1000./FRAMES_PER_SECOND;
            if(timer > 2 * 1000) {
                fade=0;
                timer=0;
                gameState=0;
            }
        }
    }

    public void drawGame(Graphics g) {
        if(back == null) back = (BufferedImage)(createImage(getWidth(), getHeight()));
        Graphics2D g2D = back.createGraphics();

        //background
        g2D.setColor(new Color(70, 70, 70));
        g2D.fillRect(0, 0, getWidth(), getHeight());

        drawSky(g2D);
        drawSurfaces(g2D);
        drawSprites(g2D);

        if (gameState==2){
            movePlayer();
        }
        ((Graphics2D)g).drawImage(back, null, 0, 0);
    }
    private void movePlayer() {
        //a
        if (keys[0]) {
            playerAngle+=PLAYER_TURN_SPEED;
            playerAngle=fixAngle(playerAngle);
            playerDeltaX=Math.cos(degreeToRadian(playerAngle));
            playerDeltaY=-Math.sin(degreeToRadian(playerAngle));
        }
        //d
        if (keys[1]) {
            playerAngle-=PLAYER_TURN_SPEED;
            playerAngle=fixAngle(playerAngle);
            playerDeltaX=Math.cos(degreeToRadian(playerAngle));
            playerDeltaY=-Math.sin(degreeToRadian(playerAngle));
        }

        int xOffset;
        if(playerDeltaX<0) {xOffset=-15;}
        else{xOffset=15;}

        int yOffset;
        if(playerDeltaY<0) {yOffset=-15;}
        else{yOffset=15;}

        int mapPlayerX = (int)(playerX)/64;
        int mapPlayerXAddXOffset = (int)(playerX+xOffset)/64;
        int mapPlayerXSubXOffset = (int)(playerX-xOffset)/64;

        int mapPlayerY = (int)(playerY)/64;
        int mapPlayerYAddYOffset = (int)(playerY+yOffset)/64;
        int mapPlayerYSubYOffset = (int)(playerY-yOffset)/64;

        //w
        if (keys[2]) {
            if(MAP_WALLS[mapPlayerY][mapPlayerXAddXOffset] == 0) {
                playerX += playerDeltaX * PLAYER_MOVE_SPEED;}
            if(MAP_WALLS[mapPlayerYAddYOffset][mapPlayerX] == 0) {
                playerY += playerDeltaY * PLAYER_MOVE_SPEED;}
        }
        //s
        if (keys[3]) {
            if(MAP_WALLS[mapPlayerY][mapPlayerXSubXOffset] == 0) {
                playerX -= playerDeltaX * PLAYER_MOVE_SPEED;}
            if(MAP_WALLS[mapPlayerYSubYOffset][mapPlayerX] == 0) {
                playerY -= playerDeltaY * PLAYER_MOVE_SPEED;}
        }
        //e - interact
        if(keys[4]) {
            if(playerDeltaX<0) {xOffset=-20;}else{xOffset=20;}
            if(playerDeltaY<0) {yOffset=-20;}else{yOffset=20;}

            mapPlayerXAddXOffset = (int)(playerX+xOffset)/64;
            mapPlayerYAddYOffset = (int)(playerY+yOffset)/64;

            if(MAP_WALLS[mapPlayerYAddYOffset][mapPlayerXAddXOffset] == 2
                    && !sprites[0].on) {
                MAP_WALLS[mapPlayerYAddYOffset][mapPlayerXAddXOffset] = 0;
            }
        }


        //if top left corner win
        if((int)playerX>>6==1 && (int)playerY>>6==1) {
            fade=0;
            timer=0;
            gameState=3;
        }
    }
    private void drawSky(Graphics2D g2D) {
        for(int y = 0; y < 40; y++) {
            for(int x = 0; x < 120; x++) {
                int xOffset = (int)playerAngle*2-x;
                if(xOffset<0){xOffset+=120;}
                xOffset=xOffset%120;
                int pixel = (y*120+xOffset)*3;
                int red = (int)(skyTexture[pixel]*fade);
                int green = (int)(skyTexture[pixel+1]*fade);
                int blue = (int)(skyTexture[pixel+2]*fade);

                g2D.setColor(new Color(red, green, blue));
                g2D.fill(new Rectangle2D.Double(x*PIXEL_SIZE,
                        y*PIXEL_SIZE, PIXEL_SIZE, PIXEL_SIZE));
            }
        }
    }
    private void drawScreen(Graphics2D g2D, int v) {
        int[] screen;
        if(v == 1) {
            screen = titleTexture;
        }else if (v == 2) {
            screen=winTexture;
        }else if (v == 3) {
            screen=loseTexture;
        }else {
            screen = new int[0];
        }
        for(int y = 0; y < 80; y++) {
            for(int x=0; x < 120; x++) {
                int pixel = (y*120+x)*3;
                int red = (int)(screen[pixel]*fade);
                int green = (int)(screen[pixel+1]*fade);
                int blue = (int)(screen[pixel+2]*fade);

                g2D.setColor(new Color(red, green, blue));
                g2D.fill(new Rectangle2D.Double(x*PIXEL_SIZE,
                        y*PIXEL_SIZE, PIXEL_SIZE, PIXEL_SIZE));
            }
        }
    }

    private void drawSprites(Graphics2D g2D) {
        for (Sprite sprite : sprites) {
            //pick up key
            if (sprite.type == 1) {
                if (playerX < sprite.x + 30 && playerX > sprite.x - 30 &&
                        playerY < sprite.y + 30 && playerY > sprite.y - 30) {
                    sprite.on = false;
                }
            }

            //enemy kills
            if (sprite.type == 3) {
                if (playerX < sprite.x + 30 && playerX > sprite.x - 30 &&
                        playerY < sprite.y + 30 && playerY > sprite.y - 30) {
                    gameState = 4;
                }

                int mapSpriteX = (int) sprite.x >> 6;
                int mapSpriteY = (int) sprite.y >> 6;
                int mapSpriteXAddOffset = ((int) sprite.x + 15) >> 6;
                int mapSpriteYAddOffset = ((int) sprite.y + 15) >> 6;
                int mapSpriteXSubOffset = ((int) sprite.x - 15) >> 6;
                int mapSpriteYSubOffset = ((int) sprite.y - 15) >> 6;


                if (sprite.x > playerX && MAP_WALLS[mapSpriteY][mapSpriteXSubOffset] == 0) {
                    sprite.x -= ENEMY_MOVE_SPEED;
                }
                if (sprite.x < playerX && MAP_WALLS[mapSpriteY][mapSpriteXAddOffset] == 0) {
                    sprite.x += ENEMY_MOVE_SPEED;
                }
                if (sprite.y > playerY && MAP_WALLS[mapSpriteYSubOffset][mapSpriteX] == 0) {
                    sprite.y -= ENEMY_MOVE_SPEED;
                }
                if (sprite.y < playerY && MAP_WALLS[mapSpriteYAddOffset][mapSpriteX] == 0) {
                    sprite.y += ENEMY_MOVE_SPEED;
                }
            }

            double spriteX = sprite.x - playerX;
            double spriteY = sprite.y - playerY;
            double spriteZ = sprite.z;

            //rotate around origin
            double playerAngleCos = Math.cos(degreeToRadian(playerAngle));
            double playerAngleSin = Math.sin(degreeToRadian(playerAngle));

            double a = spriteY * playerAngleCos + spriteX * playerAngleSin;
            double zDepth = spriteX * playerAngleCos - spriteY * playerAngleSin;
            spriteX = a;
            spriteY = zDepth;

            spriteX = (spriteX * 108.0 / spriteY) + (120 / 2.);
            spriteY = (spriteZ * 108.0 / spriteY) + (80 / 2.);

            int scale = (int) (32 * 80 / zDepth);
            if (scale < 0) {
                scale = 0;
            }
            if (scale > 120) {
                scale = 120;
            }

            double textureX = 0, textureY = 31, textureXStep = 31.5 / (double) scale, textureYStep = 32.0 / (double) scale;

            for (double x = spriteX - scale / 2.; x < spriteX + scale / 2.; x++) {
                textureY = 31;
                for (double y = 0; y < scale; y++) {
                    if (sprite.on && x > 0 && x < 120 && zDepth < rayDepth[(int) x]) {
                        int pixel = ((int) textureY * 32 + (int) textureX) * 3 + sprite.texture * 32 * 32 * 3;
                        int red = (int) (spritesTexture[pixel] * fade);
                        int green = (int) (spritesTexture[pixel + 1] * fade);
                        int blue = (int) (spritesTexture[pixel + 2] * fade);

                        //not special purple
                        if (red != (int) (255 * fade) || green != 0 * fade || blue != (int) (255 * fade)) {
                            g2D.setColor(new Color(red, green, blue));
                            g2D.fill(new Rectangle2D.Double(x * PIXEL_SIZE, spriteY * PIXEL_SIZE - y * PIXEL_SIZE,
                                    PIXEL_SIZE, PIXEL_SIZE));
                        }
                        textureY -= textureYStep;
                        if (textureY < 0) {
                            textureY = 0;
                        }
                    }
                }
                textureX += textureXStep;
            }
        }
    }

    private void drawSurfaces(Graphics2D g2D) {
        int depthOfField, mapX, mapY;
        double vertX, vertY, rayX=0, rayY=0, rayAngle, xOffset=0, yOffset=0, vertDist, horDist;

        rayAngle = fixAngle(playerAngle+30);

        for(int r = 0; r < 120; r++) {
            int vertMapTexture=0, horMapTexture=0;

            //---Vertical---
            depthOfField = 0;
            vertDist = Double.MAX_VALUE;
            double rayAngleTan =
                    Math.tan(degreeToRadian(rayAngle));

            //looking left
            if(rayAngle > 90 && rayAngle < 270) {
                rayX=(((int)playerX>>6)<<6) -0.0001;
                rayY=(playerX-rayX)*rayAngleTan+playerY;
                xOffset=-64;
                yOffset=-xOffset*rayAngleTan;
            }
            //looking right
            if(rayAngle > 270 || rayAngle < 90) {
                rayX=(((int)playerX>>6)<<6)+64;
                rayY=(playerX-rayX)*rayAngleTan+playerY;
                xOffset=64;
                yOffset=-xOffset*rayAngleTan;
            }
            //looking up or down
            if(rayAngle == 90 || rayAngle == 270) {
                rayX=playerX;
                rayY=playerY;
                depthOfField=8;
            }

            while (depthOfField < 8) {
                mapX=(int)(rayX)>>6;
                mapY=(int)(rayY)>>6;

                int mapHeight = MAP_WALLS.length;
                int mapWidth = MAP_WALLS[0].length;

                //vert hit
                if((mapX < mapWidth && mapX >= 0) && ((mapY < mapHeight) && mapY >= 0) &&
                        MAP_WALLS[mapY][mapX]>0)
                {
                    vertMapTexture=MAP_WALLS[mapY][mapX]-1;
                    vertDist = distance(playerX, playerY, rayX, rayY, rayAngle);
                    depthOfField=8;
                }
                //check next vertical
                else{
                    rayX+=xOffset;
                    rayY+=yOffset;
                    depthOfField+=1;
                }
            }
            vertX=rayX;
            vertY=rayY;

            //---Horizontal---
            depthOfField = 0;
            horDist = Double.MAX_VALUE;
            rayAngleTan = 1.0/rayAngleTan;

            //looking up
            if(rayAngle < 180){
                rayY=(((int)playerY>>6)<<6) -0.0001;
                rayX=(playerY-rayY)*rayAngleTan+playerX;
                yOffset=-64;
                xOffset=-yOffset*rayAngleTan;
            }
            //looking down
            if(rayAngle > 180){
                rayY=(((int)playerY>>6)<<6)+64;
                rayX=(playerY-rayY)*rayAngleTan+playerX;
                yOffset=64;
                xOffset=-yOffset*rayAngleTan;
            }
            //looking straight left or right
            if(rayAngle == 180 || rayAngle == 0){
                rayX=playerX;
                rayY=playerY;
                depthOfField=8;
            }

            while(depthOfField<8)
            {
                mapX=(int)(rayX)>>6;
                mapY=(int)(rayY)>>6;

                int mapHeight = MAP_WALLS.length;
                int mapWidth = MAP_WALLS[0].length;

                //hit
                if((mapX < mapWidth && mapX >= 0) && ((mapY < mapHeight) && mapY >= 0) &&
                        MAP_WALLS[mapY][mapX]>0){
                    horMapTexture=MAP_WALLS[mapY][mapX]-1;
                    depthOfField=8;
                    horDist=distance(playerX, playerY, rayX, rayY, rayAngle);
                }
                //check next horizontal
                else{
                    rayX+=xOffset;
                    rayY+=yOffset;
                    depthOfField+=1;
                }
            }

            double shade = 1;
            g2D.setColor(new Color(0,204,0));
            //horizontal hit first
            if(vertDist<horDist){
                horMapTexture=vertMapTexture;
                shade=0.5;
                rayX=vertX;
                rayY=vertY;
                horDist=vertDist;
                g2D.setColor(new Color(0,153,0));
            }

            //fix fisheye
            int cosineAngle = (int)fixAngle(playerAngle-rayAngle);

            horDist=horDist*Math.cos(degreeToRadian(cosineAngle));
            int mapSize = MAP_WALLS.length * MAP_WALLS[0].length;
            int lineHeight = (int)((mapSize*640)/(horDist));
            double textureYStep=32.0/(float)lineHeight;
            double textureYOffset=0;

            //line height and limit
            if(lineHeight>640){
                textureYOffset=(lineHeight-640)/2.0;
                lineHeight=640;
            }
            int lineOffset = 320 - (lineHeight>>1);

            rayDepth[r]=(int)horDist;
            //draw walls
            double textureY = textureYOffset*textureYStep;
            double textureX;

            if(shade == 1) {
                textureX = (int)(rayX/2.0)%32;
                if(rayAngle>180) {
                    textureX = 31-textureX;}
            }else {
                textureX = (int)(rayY/2.0)%32;
                if(rayAngle>90 && rayAngle<270) {
                    textureX = 31-textureX;}
            }

            for(int y = 0; y < lineHeight; y++) {
                int pixel = ((int)textureY * 32 + (int)textureX) * 3+(horMapTexture*32*32*3);
                int red = (int)(tilesTexture[pixel] * shade*fade);
                int green = (int)(tilesTexture[pixel+1] * shade*fade);
                int blue = (int)(tilesTexture[pixel+2] * shade*fade);

                g2D.setColor(new Color(red, green, blue));
                g2D.fill(new Rectangle2D.Double(r*PIXEL_SIZE,
                        y+lineOffset, PIXEL_SIZE, PIXEL_SIZE));

                textureY+=textureYStep;
            }


            for(int y = lineOffset+lineHeight; y < 640; y++) {
                //draw floors

                //dy means deltaY??
                double deltaY=y-(640/2.0);
                double rayAngleDegree=degreeToRadian(rayAngle);
                double rayAngleFix=Math.cos(degreeToRadian(fixAngle(playerAngle-rayAngle)));

                // 158 = (int)((rayWidth/2.) / (Math.tan(fov/2.)));
                textureX=playerX/2 + Math.cos(rayAngleDegree)*158*2*32/deltaY/rayAngleFix;
                textureY=playerY/2 - Math.sin(rayAngleDegree)*158*2*32/deltaY/rayAngleFix;

                int mp=MAP_FLOOR[(int)(textureY/32.0)][(int)(textureX/32.0)]*32*32;

                int pixel = (((int)(textureY)&31)*32 + ((int)(textureX)&31))*3+mp*3;
                int red = (int)(tilesTexture[pixel]*0.7*fade);
                int green = (int)(tilesTexture[pixel+1]*0.7*fade);
                int blue = (int)(tilesTexture[pixel+2]*0.7*fade);

                g2D.setColor(new Color(red, green, blue));
                g2D.fill(new Rectangle2D.Double(r*PIXEL_SIZE,
                        y, PIXEL_SIZE, PIXEL_SIZE));

                //draw ceiling
                mp=MAP_CEILING[(int)(textureY/32.0)][(int)(textureX/32.0)]*32*32;

                pixel = (((int)(textureY)&31)*32 + ((int)(textureX)&31))*3+mp*3;
                red = (int)(tilesTexture[pixel]*fade);
                green = (int)(tilesTexture[pixel+1]*fade);
                blue = (int)(tilesTexture[pixel+2]*fade);

                if(mp>0) {
                    g2D.setColor(new Color(red, green, blue));
                    g2D.fill(new Rectangle2D.Double(r*PIXEL_SIZE,
                            640-y, PIXEL_SIZE, PIXEL_SIZE));
                }
            }

            rayAngle = fixAngle(rayAngle-0.5);
        }
    }

    private double distance(double x1, double y1, double x2, double y2, double angle) {
        return Math.cos(degreeToRadian(angle))*(x2-x1)-Math.sin(degreeToRadian(angle))*(y2-y1);
    }
    private double fixAngle(double angle) {
        if(angle>359) {angle-=360;}
        if(angle<0) {angle+=360;}
        return angle;
    }

    private double degreeToRadian(double a) {
        return a*(Math.PI/180.0);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A) {keys[0] = true;}
        if (e.getKeyCode() == KeyEvent.VK_D) {keys[1] = true;}
        if (e.getKeyCode() == KeyEvent.VK_W) {keys[2] = true;}
        if (e.getKeyCode() == KeyEvent.VK_S) {keys[3] = true;}
        if (e.getKeyCode() == KeyEvent.VK_E) {keys[4] = true;}
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A) {keys[0] = false;}
        if (e.getKeyCode() == KeyEvent.VK_D) {keys[1] = false;}
        if (e.getKeyCode() == KeyEvent.VK_W) {keys[2] = false;}
        if (e.getKeyCode() == KeyEvent.VK_S) {keys[3] = false;}
        if (e.getKeyCode() == KeyEvent.VK_E) {keys[4] = false;}
    }

    public void run()
    {
        try
        {
            while(true)
            {
                Thread.currentThread().sleep(1000/FRAMES_PER_SECOND);
                repaint();
            }
        }catch(Exception e)
        {
            System.out.println(e);
        }
    }
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Raycast");
        frame.setSize(WIDTH, HEIGHT);

        Raycast game = new Raycast();
        (game).setFocusable(true);

        frame.getContentPane().add(game);

        frame.setVisible(true);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}

class Sprite {
    int type; //static, key, enemy
    boolean on; //on, off
    int texture; //texture to show
    double x, y, z; //position

    public Sprite(int type, boolean on, int texture, double x, double y, double z) {
        this.type = type;
        this.on = on;
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}