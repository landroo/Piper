package org.landroo.piper;

import java.util.ArrayList;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class PiperClass
{
	private static final String TAG = "PiperClass";
	private static final int ROT_STEP = 5;
	private static final float EDGE = 0.1f;
	private static final float FLUID_INC = 0.005f;
	private static final int DIFFICULTY = 9;

	private static final int pipeLine = 0;
	private static final int pipeElbow = 1;
	private static final int pipeT = 2;
	private static final int pipeX = 3;
	private static final int pipeEnd = 4;
	private static final int pipeEmpty = 5;

	private static final int wayLeft = 1;
	private static final int wayRight = 2;
	private static final int wayUp = 4;
	private static final int wayDown = 8;

	private OPMethod mOPMethod = null;// maze creator

	public int mTableWidth;// playground width
	public int mTableHeight;// playground height

	private int miCellWidth;// cell width
	private int miCellHeight;// cell height

	private ArrayList<ArrayList<Integer>> maCellData = null;// maze values
	public ArrayList<Tile> tiles = new ArrayList<Tile>();// tile list

	private Bitmap[] pipes = new Bitmap[6];
	private Bitmap[] fluid = new Bitmap[6];

	private int endGame = 0;
	public int pipeCnt = 0;

	private int difficulty = DIFFICULTY;
	private float fluidSpeed = FLUID_INC;

	public boolean ready = false;
	
	private int shadowColor;
	
	// tile data class
	public class Tile
	{
		public int id;
		public int type;

		public boolean visible = true;
		public boolean fix = false;

		public float tilPosX = 0;// actual position
		public float tilPosY = 0;

		public Drawable pipeDrawable;// bitmap
		public Bitmap pipeBitmap;

		public Drawable fluidDrawable;
		public Bitmap fluidBitmap;

		public float tilRot = 0;// rotations
		public float desRot = -1;
		public float oriRot = 0;

		public float fluidPercent = 0;
		public int direction = 0;
		public int parentWay = 0;
	}

	public PiperClass(int tableWidth, int tableHeight, int iCellWidth, int iCellHeight, int pipeColor, int shadowColor, int fillColor)
	{
		initClass(tableWidth, tableHeight, iCellWidth, iCellHeight, pipeColor, shadowColor, fillColor);	
	}
	
	public void initClass(int tableWidth, int tableHeight, int iCellWidth, int iCellHeight, int pipeColor, int shadowColor, int fillColor)
	{
		this.miCellWidth = iCellWidth;
		this.miCellHeight = iCellHeight;

		this.mTableWidth = tableWidth / this.miCellWidth;
		this.mTableHeight = tableHeight / this.miCellHeight;
		
		this.shadowColor = shadowColor;

		pipes[0] = darwPipe1(miCellWidth, miCellHeight, pipeColor, shadowColor, false);
		pipes[1] = darwPipe2(miCellWidth, miCellHeight, pipeColor, shadowColor);
		pipes[2] = darwPipe3(miCellWidth, miCellHeight, pipeColor, shadowColor);
		pipes[3] = darwPipe4(miCellWidth, miCellHeight, pipeColor, shadowColor);
		pipes[4] = darwPipe1(miCellWidth, miCellHeight, pipeColor, shadowColor, true);
		pipes[5] = Bitmap.createBitmap(iCellWidth, iCellHeight, Bitmap.Config.ARGB_4444);

		fluid[0] = darwPipe1(miCellWidth, miCellHeight, fillColor, shadowColor, false);
		fluid[1] = darwPipe2(miCellWidth, miCellHeight, fillColor, shadowColor);
		fluid[2] = darwPipe3(miCellWidth, miCellHeight, fillColor, shadowColor);
		fluid[3] = darwPipe4(miCellWidth, miCellHeight, fillColor, shadowColor);
		fluid[4] = darwPipe1(miCellWidth, miCellHeight, fillColor, shadowColor, true);
		fluid[5] = Bitmap.createBitmap(iCellWidth, iCellHeight, Bitmap.Config.ARGB_4444);
		
		this.mOPMethod = new OPMethod(mTableWidth - 1, mTableHeight, miCellWidth, miCellHeight);
		
		return;
	}
	
	public boolean newMaze(boolean bFirst)
	{
		ready = false;

		if (bFirst)
		{
			difficulty = DIFFICULTY;
			fluidSpeed = FLUID_INC;
			pipeCnt = 0;
		}
		else
		{
			if (difficulty > 1) difficulty--;
			if (fluidSpeed < 0.05f) fluidSpeed += 0.001f;
		}

		tiles = new ArrayList<Tile>();
		endGame = 0;

		try
		{
			this.maCellData = this.mOPMethod.createLabyrinth();
		}
		catch(Exception ex)
		{
			Log.i(TAG, "" + ex);
			return false;
		}
		
		int type, rot;
		boolean fix = false;
		for (int x = 0; x < this.mTableWidth; x++)
		{
			for (int y = 0; y < this.mTableHeight; y++)
			{
				if (x == this.mTableWidth - 1)
				{
					type = 6;
					rot = 90;
					fix = true;
				}
				else
				{
					type = getMazeType(x, y, mTableWidth - 1, mTableHeight);
					rot = getAngle(type);
					fix = false;
				}
				tiles.add(addTile(x, y, type, rot, fix));
			}
		}
	
		modTile(this.mTableWidth - 1, this.mTableHeight - 1, 13, 90, true);
		modTile(this.mTableWidth - 1, 0, 2, 270, true);

		int idx = (this.mTableWidth - 2) * this.mTableHeight + 0;
		Tile tile = tiles.get(idx);
		// Log.i(TAG, "Tile type: " + tile.type);
		if (tile.type == 2) modTile(this.mTableWidth - 2, 0, 7, 180, false);// from left to down -> from left down and right
		if (tile.type == 13) modTile(this.mTableWidth - 2, 0, 1, 180, false);// from down end -> L
		if (tile.type == 11) modTile(this.mTableWidth - 2, 0, 5, 0, false);// from right end -> I

		ready = true;

		return true;
	}

	private Tile addTile(int x, int y, int type, int rot, boolean fix)
	{
		Tile tile = new Tile();
		tile.type = type;
		tile.tilPosX = x * miCellWidth;
		tile.tilPosY = y * miCellHeight;
		tile.tilRot = rot;
		tile.fix = fix;

		tile.id = tiles.size();

		tile.pipeBitmap = pipes[getPipeType(tile.type)];
		tile.pipeDrawable = new BitmapDrawable(tile.pipeBitmap);
		tile.pipeDrawable.setBounds(0, 0, miCellWidth, miCellHeight);

		tile.fluidBitmap = Bitmap.createBitmap(miCellWidth, miCellHeight, Bitmap.Config.ARGB_4444);
		tile.fluidDrawable = new BitmapDrawable(tile.fluidBitmap);
		tile.fluidDrawable.setBounds(0, 0, miCellWidth, miCellHeight);

		return tile;
	}

	private void modTile(int x, int y, int type, int rot, boolean fix)
	{
		int idx = x * this.mTableHeight + y;
		Tile tile = tiles.get(idx);
		tile.type = type;
		tile.tilRot = rot;
		tile.fix = fix;
		tile.pipeBitmap = pipes[getPipeType(type)];
		tile.pipeDrawable = new BitmapDrawable(tile.pipeBitmap);
		tile.pipeDrawable.setBounds(0, 0, miCellWidth, miCellHeight);

		return;
	}

	// line
	private Bitmap darwPipe1(int w, int h, int pipeColor, int shadowColor, boolean half)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setAntiAlias(true);

		int[] colors = new int[5];
		colors[0] = shadowColor;
		colors[1] = shadowColor;
		colors[2] = pipeColor;
		colors[3] = shadowColor;
		colors[4] = shadowColor;

		// end pipe
		if (half)
		{
			int[] cols = new int[2];
			cols[0] = pipeColor;
			cols[1] = shadowColor;

			RadialGradient gradient = new RadialGradient(w / 2, h / 2, w / 4, cols, null,
					android.graphics.Shader.TileMode.CLAMP);
			paint.setShader(gradient);
			paint.setStyle(Paint.Style.FILL);

			canvas.drawCircle(w / 2, h / 2, w / 4, paint);
		}

		LinearGradient gradient = new LinearGradient(0, 0, 0, h, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setStyle(Paint.Style.STROKE);
		paint.setShader(gradient);
		paint.setStrokeWidth(w / 2);

		// end pipe
		if (half)
		{
			canvas.drawLine(0, h / 2, w / 2, h / 2, paint);
		}
		else
		{
			canvas.drawLine(0, h / 2, w, h / 2, paint);
			drawEdge(bitmap, 2, pipeColor, shadowColor, EDGE);
		}
		drawEdge(bitmap, 1, pipeColor, shadowColor, EDGE);

		return bitmap;
	}

	// curve
	private Bitmap darwPipe2(int w, int h, int color, int shadowColor)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
		bitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);

		int[] colors = new int[5];
		colors[0] = shadowColor;
		colors[1] = shadowColor;
		colors[2] = color;
		colors[3] = shadowColor;
		colors[4] = shadowColor;

		RadialGradient gradient = new RadialGradient(0, 0, w, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);
		paint.setStrokeWidth(w / 2);

		// float ox = w / (EDGE * 100 / 2);
		// float oy = h / (EDGE * 100 / 2);
		float ox = 0;
		float oy = 0;

		RectF rect = new RectF();
		rect.set(-w / 2 + ox, -h / 2 + oy, w / 2, h / 2);
		canvas.drawArc(rect, 0, 90, false, paint);

		drawEdge(bitmap, 1, color, shadowColor, EDGE);
		drawEdge(bitmap, 3, color, shadowColor, EDGE);

		return bitmap;
	}

	// three direction
	private Bitmap darwPipe3(int w, int h, int color, int shadowColor)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
		bitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);

		int[] colors = new int[5];
		colors[0] = shadowColor;
		colors[1] = shadowColor;
		colors[2] = color;
		colors[3] = color;
		colors[4] = color;

		paint.setStrokeWidth(w / 2);
		RadialGradient gradient = new RadialGradient(0, 0, w, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);

		RectF rect = new RectF();
		rect.set(-w / 2, -h / 2, w / 2, h / 2);
		canvas.drawArc(rect, 0, 90, false, paint);

		Bitmap corner1 = Bitmap.createBitmap(w / 2, h / 2, Bitmap.Config.ARGB_4444);
		Canvas canv1 = new Canvas(corner1);
		Rect src = new Rect(0, 0, w / 2, h / 2);
		Rect dst = new Rect(0, 0, w / 2, h / 2);
		canv1.drawBitmap(bitmap, src, dst, paint);
		Matrix matrix = new Matrix();
		matrix.setRotate(0, corner1.getWidth() / 2, corner1.getHeight() / 2);

		Bitmap corner2 = Bitmap.createBitmap(w / 2, h / 2, Bitmap.Config.ARGB_4444);
		Canvas canv2 = new Canvas(corner2);
		canv2.drawBitmap(corner1, matrix, paint);

		colors[0] = shadowColor;
		colors[1] = shadowColor;
		colors[2] = color;
		colors[3] = shadowColor;
		colors[4] = shadowColor;

		LinearGradient grad = new LinearGradient(0, 0, 0, h, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(grad);
		paint.setStrokeWidth(w / 2);

		canvas.drawLine(0, h / 2, w, h / 2, paint);

		canvas.drawBitmap(corner2, 0, 0, paint);

		corner2 = Bitmap.createBitmap(w / 2, h / 2, Bitmap.Config.ARGB_4444);
		canv2 = new Canvas(corner2);

		matrix.setRotate(90, corner1.getWidth() / 2, corner1.getHeight() / 2);
		canv2.drawBitmap(corner1, matrix, paint);

		canvas.drawBitmap(corner2, w / 2, 0, paint);

		drawEdge(bitmap, 1, color, shadowColor, EDGE);
		drawEdge(bitmap, 2, color, shadowColor, EDGE);
		drawEdge(bitmap, 3, color, shadowColor, EDGE);

		return bitmap;
	}

	// four direction
	private Bitmap darwPipe4(int w, int h, int color, int shadowColor)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
		bitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(bitmap);
		RectF rect = new RectF();

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);

		int[] colors = new int[5];
		colors[0] = shadowColor;
		colors[1] = shadowColor;
		colors[2] = color;
		colors[3] = color;
		colors[4] = color;

		paint.setStrokeWidth(w / 2);
		RadialGradient gradient = new RadialGradient(0, 0, w, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);

		rect.set(-w / 2, -h / 2, w / 2, h / 2);
		canvas.drawArc(rect, 0, 90, false, paint);

		gradient = new RadialGradient(w, h, w, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);

		rect.set(w / 2, h / 2, w + w / 2, h + h / 2);
		canvas.drawArc(rect, 180, 90, false, paint);

		Bitmap corner1 = Bitmap.createBitmap(w / 2, h / 2, Bitmap.Config.ARGB_4444);
		Canvas canv1 = new Canvas(corner1);
		Rect src = new Rect(0, 0, w / 2, h / 2);
		Rect dst = new Rect(0, 0, w / 2, h / 2);
		canv1.drawBitmap(bitmap, src, dst, paint);
		Matrix matrix = new Matrix();
		matrix.setRotate(270, corner1.getWidth() / 2, corner1.getHeight() / 2);

		Bitmap corner2 = Bitmap.createBitmap(w / 2, h / 2, Bitmap.Config.ARGB_4444);
		Canvas canv2 = new Canvas(corner2);
		canv2.drawBitmap(corner1, matrix, paint);

		canvas.drawBitmap(corner2, 0, h / 2, paint);

		corner2 = Bitmap.createBitmap(w / 2, h / 2, Bitmap.Config.ARGB_4444);
		canv2 = new Canvas(corner2);

		matrix.setRotate(90, corner1.getWidth() / 2, corner1.getHeight() / 2);
		canv2.drawBitmap(corner1, matrix, paint);

		canvas.drawBitmap(corner2, w / 2, 0, paint);

		colors[0] = shadowColor;
		colors[1] = shadowColor;
		colors[2] = color;
		colors[3] = shadowColor;
		colors[4] = shadowColor;

		drawEdge(bitmap, 1, color, shadowColor, EDGE);
		drawEdge(bitmap, 2, color, shadowColor, EDGE);
		drawEdge(bitmap, 3, color, shadowColor, EDGE);
		drawEdge(bitmap, 4, color, shadowColor, EDGE);

		return bitmap;
	}

	// pos: 1 left, 2 right, 3 top, 4 bottom
	private void drawEdge(Bitmap bitmap, int pos, int color, int edgeColor, float percent)
	{
		Paint paint = new Paint();
		Canvas canvas = new Canvas(bitmap);

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();

		int[] colors = new int[7];
		colors[0] = edgeColor;
		colors[1] = edgeColor;
		colors[2] = edgeColor;
		colors[3] = color;
		colors[4] = edgeColor;
		colors[5] = edgeColor;
		colors[6] = edgeColor;

		LinearGradient gradient = new LinearGradient(0, 0, 0, h, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setStyle(Paint.Style.STROKE);
		paint.setShader(gradient);
		paint.setStrokeWidth(w / 2);

		int ew = (int) (w * percent);
		int eh = (int) (h * percent);
		paint.setStrokeWidth(w / 3 * 2);
		if (pos == 1) canvas.drawLine(1, h / 2, ew, h / 2, paint);
		if (pos == 2) canvas.drawLine(w - ew, h / 2, w - 1, h / 2, paint);

		gradient = new LinearGradient(0, 0, w, 0, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);

		if (pos == 3) canvas.drawLine(w / 2, 1, w / 2, eh, paint);
		if (pos == 4) canvas.drawLine(w / 2, h - eh, w / 2, h - 1, paint);

		paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(w / 2);
		paint.setColor(0xFF000000);

		switch (pos)
		{
		case 1:
			canvas.drawLine(ew, h / 2, ew + 1, h / 2, paint);
			canvas.drawLine(0, h / 2, 1, h / 2, paint);
			break;
		case 2:
			canvas.drawLine(w - ew - 1, h / 2, w - ew, h / 2, paint);
			canvas.drawLine(w - 1, h / 2, w, h / 2, paint);
			break;
		case 3:
			canvas.drawLine(w / 2, 0, w / 2, 1, paint);
			canvas.drawLine(w / 2, eh, w / 2, eh + 1, paint);
			break;
		case 4:
			canvas.drawLine(w / 2, h - eh - 1, w / 2, h - eh, paint);
			canvas.drawLine(w / 2, h - 1, w / 2, h, paint);
			break;
		}

		// drills
		paint.setStrokeWidth(w / 20);
		paint.setColor(edgeColor);
		switch (pos)
		{
		case 1:
			canvas.drawLine(ew, h / 5 * 1, ew + ew / 2, h / 5 * 1, paint);
			canvas.drawLine(ew, h / 5 * 2, ew + ew / 2, h / 5 * 2, paint);
			canvas.drawLine(ew, h / 5 * 3, ew + ew / 2, h / 5 * 3, paint);
			canvas.drawLine(ew, h / 5 * 4, ew + ew / 2, h / 5 * 4, paint);
			break;
		case 2:
			canvas.drawLine(w - ew, h / 5 * 1, w - ew - ew / 2, h / 5 * 1, paint);
			canvas.drawLine(w - ew, h / 5 * 2, w - ew - ew / 2, h / 5 * 2, paint);
			canvas.drawLine(w - ew, h / 5 * 3, w - ew - ew / 2, h / 5 * 3, paint);
			canvas.drawLine(w - ew, h / 5 * 4, w - ew - ew / 2, h / 5 * 4, paint);
			break;
		case 3:
			canvas.drawLine(w / 5 * 1, eh, w / 5 * 1, eh + eh / 2, paint);
			canvas.drawLine(w / 5 * 2, eh, w / 5 * 2, eh + eh / 2, paint);
			canvas.drawLine(w / 5 * 3, eh, w / 5 * 3, eh + eh / 2, paint);
			canvas.drawLine(w / 5 * 4, eh, w / 5 * 4, eh + eh / 2, paint);
			break;
		case 4:
			canvas.drawLine(w / 5 * 1, h - eh, w / 5 * 1, h - eh - eh / 2, paint);
			canvas.drawLine(w / 5 * 2, h - eh, w / 5 * 2, h - eh - eh / 2, paint);
			canvas.drawLine(w / 5 * 3, h - eh, w / 5 * 3, h - eh - eh / 2, paint);
			canvas.drawLine(w / 5 * 4, h - eh, w / 5 * 4, h - eh - eh / 2, paint);
			break;
		}
	}

	// return with a cell type
	public int getMazeType(int x, int y, int tableWidth, int tableHeight)
	{
		int iRet = 0;
		boolean bLeft = false;
		boolean bRight = false;
		boolean bUp = false;
		boolean bDown = false;

		// left wall
		if ((this.maCellData.get(x).get(y) & 2) == 0) bLeft = true;
		// right wall or end
		if (x + 1 == tableWidth) bRight = true;
		else if ((this.maCellData.get(x + 1).get(y) & 2) == 0) bRight = true;
		// upward wall
		if ((this.maCellData.get(x).get(y) & 1) == 0) bUp = true;
		// downward wall or end
		if (y + 1 == tableHeight) bDown = true;
		else if ((this.maCellData.get(x).get(y + 1) & 1) == 0) bDown = true;

		if (bLeft && !bRight && bUp && !bDown) iRet = 1;// L from right to down way, left up wall
		if (!bLeft && bRight && bUp && !bDown) iRet = 2;// L from left to down way, right, up wall
		if (!bLeft && bRight && !bUp && bDown) iRet = 3;// L from up to left way, right up wall
		if (bLeft && !bRight && !bUp && bDown) iRet = 4;// L from up to right way, left down wall

		if (!bLeft && !bRight && bUp && bDown) iRet = 5;// I horizontal way, up, down wall
		if (bLeft && bRight && !bUp && !bDown) iRet = 6;// I vertical way left, right wall

		if (!bLeft && !bRight && bUp && !bDown) iRet = 7;// T left, down and right way, up wall
		if (bLeft && !bRight && !bUp && !bDown) iRet = 8;// T up, right and down way, left wall
		if (!bLeft && bRight && !bUp && !bDown) iRet = 9;// T up, left and down way, right wall
		if (!bLeft && !bRight && !bUp && bDown) iRet = 10;// T up, right and left way, down wall

		if (!bLeft && bRight && bUp && bDown) iRet = 11;// E way from right
		if (bLeft && !bRight && bUp && bDown) iRet = 12;// E way from left
		if (bLeft && bRight && bUp && !bDown) iRet = 13;// E way from down
		if (bLeft && bRight && !bUp && bDown) iRet = 14;// E way from up

		if (!bLeft && !bRight && !bUp && !bDown) iRet = 15;// + no wall

		return iRet;
	}

	private int getAngle(int iType)
	{
		switch (iType)
		{
		case 1:
			return 180;
		case 2:
			return 270;
		case 3:
			return 0;
		case 4:
			return 90;
		case 5:
			return 0;
		case 6:
			return 90;
		case 7:
			return 180;
		case 8:
			return 90;
		case 9:
			return 270;
		case 10:
			return 0;
		case 11:
			return 0;
		case 12:
			return 180;
		case 13:
			return 270;
		case 14:
			return 90;
		case 15:
			return 0;
		}

		return 0;
	}

	private int getPipeType(int iType)
	{
		switch (iType)
		{
		case 0:
			return pipeEmpty;// empty
		case 1:
			return pipeElbow;// L
		case 2:
			return pipeElbow;// L
		case 3:
			return pipeElbow;// L
		case 4:
			return pipeElbow;// L
		case 5:
			return pipeLine;// I
		case 6:
			return pipeLine;// I
		case 7:
			return pipeT;// T
		case 8:
			return pipeT;// T
		case 9:
			return pipeT;// T
		case 10:
			return pipeT;// T
		case 11:
			return pipeEnd;// end
		case 12:
			return pipeEnd;// end
		case 13:
			return pipeEnd;// end
		case 14:
			return pipeEnd;// end
		case 15:
			return pipeX;// X
		}

		return 0;
	}

	public Bitmap getBackGround(int w, int h, Resources res)
	{
		Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		//bitmap.eraseColor(0xFFFF0000);
		Canvas canvas = new Canvas(bitmap);
		RectF rect = new RectF();

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);

		int color1 = 0xFF222222;
		int color2 = 0xFF882222;

		int[] colors = new int[2];
		colors[0] = color1;
		colors[1] = color2;

		float bw = w / 10;
		float bh = h / 40;
		float gap = w / 200;

		LinearGradient grad;

		for (int i = 0; i < 11; i++)
		{
			for (int j = 0; j < 40; j++)
			{
				if (random(0, 1, 1) == 1)
				{
					colors[0] = color1;
					colors[1] = color2 + random(0, 3, 1) * 0x1100;
				}
				else
				{
					colors[1] = color1;
					colors[0] = color2 + random(0, 3, 1) * 0x1100;
				}

				if (j % 2 == 0)
				{
					grad = new LinearGradient(i * bw, j * bh, i * bw + bw, j * bh + bh, colors, null,
							android.graphics.Shader.TileMode.REPEAT);
					rect.set(i * bw + gap, j * bh + gap, i * bw + bw - gap, j * bh + bh - gap);
				}
				else
				{
					grad = new LinearGradient(i * bw - bw / 2, j * bh, i * bw + bw - bw / 2, j * bh + bh, colors, null,
							android.graphics.Shader.TileMode.REPEAT);
					rect.set(i * bw + gap - bw / 2, j * bh + gap, i * bw + bw - gap - bw / 2, j * bh + bh - gap);
				}
				paint.setShader(grad);
				canvas.drawRect(rect, paint);
			}
		}
		
		paint.setShader(null);
		paint.setAlpha(64);
		
		float width = w / 2;
		float height = h / 2;
		
		float x, y, r, scaleWidth, scaleHeight;
		Bitmap img;

		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10) * 2;
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10);
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.amoba1), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		canvas.drawBitmap(img, x, y, paint);
		
		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10);
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10);
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.amoba2), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		canvas.drawBitmap(img, x + width, y + height, paint);
		
		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10);
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10);
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.jewel2), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		r = random(0, 7, 1) * 45;
		canvas.drawBitmap(rotImage(img, r), x + width, y, paint);
		
		scaleWidth = (float) width * (0.5f + ((float)random(0, 5, 1)) / 10) * 2;
		scaleHeight = (float) height * (0.5f + ((float)random(0, 5, 1)) / 10) / 2;
		img = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(res, R.drawable.colorizer), (int)scaleWidth, (int)scaleHeight, false);
		x = random(0, (int)width - img.getWidth(), 1);
		y = random(0, (int)height - img.getHeight(), 1);
		r = random(0, 7, 1) * 45;
		canvas.drawBitmap(rotImage(img, r), x, y + height, paint);

		return bitmap;
	}
	
	private Bitmap rotImage(Bitmap img, float rot)
	{
		int origWidth = img.getWidth();
		int origHeight = img.getHeight();
		Matrix matrix = new Matrix();
		matrix.setRotate(rot, img.getWidth() / 2, img.getHeight() / 2);
		Bitmap outImage = Bitmap.createBitmap(img, 0, 0, origWidth, origHeight, matrix, false);
		
		return outImage;
	}

	// add 90 degree to the selected pipe rotation
	public void startRot(float x, float y)
	{
		int tx = (int) (x / miCellWidth);
		int ty = (int) (y / miCellHeight);
		int idx = tx * this.mTableHeight + ty;
		if (tx < this.mTableWidth && ty < this.mTableHeight && idx < tiles.size())
		{
			Tile tile = tiles.get(idx);
			if (tile.desRot == -1 && tile.fluidPercent == 0 && tile.fix == false) tile.desRot = tile.tilRot + 90;
		}
	}

	// rotate all tile
	public boolean rotTiles()
	{
		if (endGame > 0) return false;

		boolean bRot = false;
		for (Tile tile : tiles)
		{
			if (tile.desRot != -1)
			{
				rotTile(tile);
				bRot = true;
			}
		}

		return bRot;
	}

	// rotate tile
	private boolean rotTile(Tile tile)
	{
		if (tile.desRot > tile.tilRot)
		{
			if (tile.desRot <= tile.tilRot + ROT_STEP)
			{
				tile.tilRot = tile.desRot % 360;
				tile.desRot = -1;
				if (checkAllPipes())
				{
					endGame = 1;
					for(Tile pipe: tiles) if(pipe.fluidPercent <= 1f) this.pipeCnt++;
				}
			}
			else tile.tilRot += ROT_STEP;

			return true;
		}

		return false;
	}

	// draw the fluid in line pipe by percent
	public void darwFluid1(Bitmap bitmap, int w, int h, int color, float percent, int dir)
	{
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		Rect src = new Rect();
		float perc = (float) w * percent;

		if (dir == 1)
		{
			src.left = 0;
			src.top = 0;
			src.right = (int) perc;
			src.bottom = h;
		}

		if (dir == 2)
		{
			src.left = w - (int) perc;
			src.top = 0;
			src.right = w;
			src.bottom = h;
		}

		canvas.drawBitmap(fluid[0], src, src, paint);
	}

	// draw fluid in elbow
	public void darwFluid2(Bitmap bitmap, int w, int h, int color, int shadowColor, float percent, int dir)
	{
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);

		int[] colors = new int[5];
		colors[0] = shadowColor;
		colors[1] = shadowColor;
		colors[2] = color;
		colors[3] = shadowColor;
		colors[4] = shadowColor;

		RadialGradient gradient = new RadialGradient(0, 0, w, colors, null, android.graphics.Shader.TileMode.CLAMP);
		paint.setShader(gradient);
		paint.setStrokeWidth(w / 2);

		float perc = (float) 90 * percent;

		// float ox = w / (EDGE * 100 / 2);
		// float oy = h / (EDGE * 100 / 2);
		float ox = 0;
		float oy = 0;

		RectF rect = new RectF();
		rect.set(-w / 2 + ox, -h / 2 + oy, w / 2, h / 2);
		if (dir == 1)
		{
			canvas.drawArc(rect, 0, perc, false, paint);
			if (percent >= .2f)
			{
				Rect src = new Rect();
				perc = (float) h * .15f;
				src.left = 0;
				src.top = 0;
				src.right = w;
				src.bottom = (int) perc;
				canvas.drawBitmap(fluid[1], src, src, paint);
			}
			if (percent >= .98f)
			{
				Rect src = new Rect();
				perc = (float) w * .15f;
				src.left = 0;
				src.top = 0;
				src.right = (int) (w - perc);
				src.bottom = h;
				canvas.drawBitmap(fluid[1], src, src, paint);
			}
		}
		if (dir == 2)
		{
			canvas.drawArc(rect, 90, -perc, false, paint);
			if (percent >= .98f)
			{
				Rect src = new Rect();
				perc = (float) h * .15f;
				src.left = 0;
				src.top = 0;
				src.right = w;
				src.bottom = (int) perc;
				canvas.drawBitmap(fluid[1], src, src, paint);
			}
			if (percent >= .2f)
			{
				Rect src = new Rect();
				perc = (float) w * .15f;
				src.left = 0;
				src.top = 0;
				src.right = (int) (perc);
				src.bottom = h;
				canvas.drawBitmap(fluid[1], src, src, paint);
			}
		}
	}

	public void darwFluid3(Bitmap bitmap, int w, int h, int color, float percent, int dir)
	{
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		Rect src = new Rect();
		float perc;

		if (dir == 1)
		{
			perc = (float) w * percent;
			src.left = 0;
			src.top = 0;
			src.right = (int) perc;
			src.bottom = h;
		}

		if (dir == 2)
		{
			perc = (float) w * percent;
			src.left = w - (int) perc;
			src.top = 0;
			src.right = w;
			src.bottom = h;
		}

		if (dir == 3)
		{
			perc = (float) (h / 4 * 3 + h * EDGE) * percent;
			src.left = 0;
			src.top = 0;
			src.right = w;
			src.bottom = (int) perc;
		}

		canvas.drawBitmap(fluid[2], src, src, paint);
	}

	public void darwFluid4(Bitmap bitmap, int w, int h, int color, float percent, int dir)
	{
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		Rect src = new Rect();
		float perc;

		if (dir == 1)
		{
			perc = (float) w * percent;
			src.left = 0;
			src.top = 0;
			src.right = (int) perc;
			src.bottom = h;
		}

		if (dir == 2)
		{
			perc = (float) w * percent;
			src.left = w - (int) perc;
			src.top = 0;
			src.right = w;
			src.bottom = h;
		}

		if (dir == 3)
		{
			perc = (float) h * percent;
			src.left = 0;
			src.top = 0;
			src.right = w;
			src.bottom = (int) perc;
		}

		if (dir == 4)
		{
			perc = (float) h * percent;
			src.left = 0;
			src.top = h - (int) perc;
			src.right = w;
			src.bottom = h;
		}

		canvas.drawBitmap(fluid[3], src, src, paint);
	}

	// pipe end dir = 1 left to right, dir = 2 right to left
	public void darwFluid5(Bitmap bitmap, int w, int h, int color, float percent, int dir)
	{
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		Rect src = new Rect();
		float perc = (float) w / 4 * 3 * percent;

		if (dir == 1)
		{
			src.left = 0;
			src.top = 0;
			src.right = (int) perc;
			src.bottom = h;
		}

		if (dir == 2)
		{
			src.left = w / 4 * 3 - (int) perc;
			src.top = 0;
			src.right = w / 4 * 3;
			src.bottom = h;
		}

		canvas.drawBitmap(fluid[4], src, src, paint);
	}

	// update fluid in pipes
	public boolean updateFluid(int color)
	{
		boolean bRet = false;
		// walk through all pipe
		for (PiperClass.Tile tile : tiles)
		{
			// check the pipe is under flow
			if (tile.fluidPercent > 0 && tile.fluidPercent < 1)
			{
				if(endGame == 0) tile.fluidPercent += fluidSpeed;
				else tile.fluidPercent += FLUID_INC * 6;
				if (tile.fluidPercent < 1)
				{
					switch (getPipeType(tile.type))
					{
					case 0:// I
						darwFluid1(tile.fluidBitmap, miCellWidth, miCellWidth, color, tile.fluidPercent, tile.direction);
						break;
					case 1:// L
						darwFluid2(tile.fluidBitmap, miCellWidth, miCellWidth, color, shadowColor, tile.fluidPercent, tile.direction);
						break;
					case 2:// T
						darwFluid3(tile.fluidBitmap, miCellWidth, miCellWidth, color, tile.fluidPercent, tile.direction);
						break;
					case 3:// +
						darwFluid4(tile.fluidBitmap, miCellWidth, miCellWidth, color, tile.fluidPercent, tile.direction);
						break;
					case 4:// end
						darwFluid5(tile.fluidBitmap, miCellWidth, miCellWidth, color, tile.fluidPercent, tile.direction);
						break;
					}
				}
				else
				{
					startNextTiles(tile);
					if(endGame == 0) pipeCnt++;
				}
				bRet = true;
			}
		}

		return bRet;
	}

	// find first pipe end and set the fluid percent bigger than 0
	public boolean startFluid(int start)
	{
		if (ready)
		{
			if (start == 0) startCell(mTableWidth - 1, mTableHeight - 1, 4);
			if (start == 1)
			{
				for (int y = 0; y < mTableHeight; y++)
					for (int x = 0; x < mTableWidth; x++)
						if (startCell(x, y, 4)) return true;
			}
			if (start == 2)
			{
				startCell(1, 3, 4);
				startCell(3, 3, 4);
				startCell(4, 3, 4);
				startCell(7, 3, 4);
				
				startCell(2, 7, 4);
				startCell(3, 7, 4);
				startCell(4, 7, 4);
				startCell(7, 7, 4);
			}
		}

		return false;
	}

	// set the start pipe fluid parameters
	private boolean startCell(int x, int y, int type)
	{
		int idx = x * this.mTableHeight + y;
		Tile tile = tiles.get(idx);
		if (getPipeType(tile.type) == type)
		{
			tile.fluidPercent = 0.01f;
			tile.direction = 2;

			return true;
		}

		return false;
	}

	// find the surroundings tiles
	private int startNextTiles(Tile tile)
	{
		int myWay = getWay(tile);
		int outWay = 0, toWay;
		int x = (int) (tile.tilPosX / miCellWidth);
		int y = (int) (tile.tilPosY / miCellHeight);
		Tile nextTile = null;
		int idx;
		boolean end = false;

		if (tile.desRot != -1)
		{
			// Log.i(TAG, "Rotating! " + tile.desRot);
			return 0;
		}

		// to left
		if ((myWay & wayLeft) == wayLeft && tile.parentWay != wayLeft)
		{
			if (x > 0)
			{
				idx = (x - 1) * this.mTableHeight + y;
				nextTile = tiles.get(idx);
				// Log.i(TAG, "to left: " + getPipeType(nextTile.type));
				toWay = getWay(nextTile);
				if ((toWay & wayRight) == wayRight)
				{
					if (nextTile.fluidPercent < 1)
					{
						nextTile.fluidPercent = 0.01f;
						nextTile.direction = getDirection(nextTile, 21);
					}
					nextTile.parentWay = wayRight;
					outWay |= wayLeft;
				}
			}
			else end = true;
		}
		// to right
		if ((myWay & wayRight) == wayRight && tile.parentWay != wayRight)
		{
			if (x < mTableWidth - 1)
			{
				idx = (x + 1) * this.mTableHeight + y;
				nextTile = tiles.get(idx);
				// Log.i(TAG, "to right: " + getPipeType(nextTile.type));
				toWay = getWay(nextTile);
				if ((toWay & wayLeft) == wayLeft)
				{
					if (nextTile.fluidPercent < 1)
					{
						nextTile.fluidPercent = 0.01f;
						nextTile.direction = getDirection(nextTile, 12);
					}
					nextTile.parentWay = wayLeft;
					outWay |= wayRight;
				}
			}
			else end = true;
		}
		// to up
		if ((myWay & wayUp) == wayUp && tile.parentWay != wayUp)
		{
			if (y > 0)
			{
				idx = x * this.mTableHeight + (y - 1);
				nextTile = tiles.get(idx);
				// Log.i(TAG, "to up: " + getPipeType(nextTile.type));
				toWay = getWay(nextTile);
				if ((toWay & wayDown) == wayDown)
				{
					if (nextTile.fluidPercent < 1)
					{
						nextTile.fluidPercent = 0.01f;
						nextTile.direction = getDirection(nextTile, 48);
					}
					nextTile.parentWay = wayDown;
					outWay |= wayUp;
				}
			}
			else end = true;
		}
		// to down
		if ((myWay & wayDown) == wayDown && tile.parentWay != wayDown)
		{
			if (y < mTableHeight - 1)
			{
				idx = x * this.mTableHeight + (y + 1);
				nextTile = tiles.get(idx);
				// Log.i(TAG, "to down: " + getPipeType(nextTile.type));
				toWay = getWay(nextTile);
				if ((toWay & wayUp) == wayUp)
				{
					if (nextTile.fluidPercent < 1)
					{
						nextTile.fluidPercent = 0.01f;
						nextTile.direction = getDirection(nextTile, 84);
					}
					nextTile.parentWay = wayUp;
					outWay |= wayDown;
				}
			}
			else end = true;
		}

		// Log.i(TAG, "Id: " + tile.id + " Next ways: " + iRet + " My ways: " +
		// myWay + " To ways: " + toWay + " End: " + end);

		if (outWay != myWay - tile.parentWay || end == true)
		{
			if (getPipeType(tile.type) != pipeEnd) endGame = 2;
		}
		else
		{
			if (checkFullPipes()) endGame = 1;
		}

		return outWay;
	}

	// open side 1 left 2 right 4 up 8 bottom
	public int getWay(Tile tile)
	{
		int iRet = 0;

		switch (getPipeType(tile.type))
		{
		case pipeLine: // I
			if (tile.tilRot == 180 || tile.tilRot == 0)
			{
				iRet |= wayLeft;
				iRet |= wayRight;
			}
			else
			{
				iRet |= wayUp;
				iRet |= wayDown;
			}
			break;
		case pipeElbow: // L
			if (tile.tilRot == 0)
			{
				iRet |= wayLeft;
				iRet |= wayUp;
			}
			else if (tile.tilRot == 90)
			{
				iRet |= wayRight;
				iRet |= wayUp;
			}
			else if (tile.tilRot == 180)
			{
				iRet |= wayRight;
				iRet |= wayDown;
			}
			else if (tile.tilRot == 270)
			{
				iRet |= wayLeft;
				iRet |= wayDown;
			}
			break;
		case pipeT: // T
			if (tile.tilRot == 0)
			{
				iRet |= wayLeft;
				iRet |= wayRight;
				iRet |= wayUp;
			}
			else if (tile.tilRot == 90)
			{
				iRet |= wayRight;
				iRet |= wayUp;
				iRet |= wayDown;
			}
			else if (tile.tilRot == 180)
			{
				iRet |= wayLeft;
				iRet |= wayRight;
				iRet |= wayDown;
			}
			else if (tile.tilRot == 270)
			{
				iRet |= wayLeft;
				iRet |= wayUp;
				iRet |= wayDown;
			}
			break;
		case pipeX: // X
			iRet |= wayLeft;
			iRet |= wayRight;
			iRet |= wayUp;
			iRet |= wayDown;
			break;
		case pipeEnd: // end
			if (tile.tilRot == 0) iRet |= wayLeft;
			else if (tile.tilRot == 90) iRet |= wayUp;
			else if (tile.tilRot == 180) iRet |= wayRight;
			else if (tile.tilRot == 270) iRet |= wayDown;
			break;
		}
		// Log.i(TAG, "Type: " + getPipeType(tile.type) + " Rot: " + tile.tilRot
		// + " Way: " + iRet);
		return iRet;
	}

	// direction to 1 left 2 right 3 up 4 bottom
	public int getDirection(Tile tile, int iNext)
	{
		int iRes = 0;

		switch (getPipeType(tile.type))
		{
		case 0: // line
			if (iNext == 12 && tile.tilRot == 0) iRes = 1;
			if (iNext == 12 && tile.tilRot == 180) iRes = 2;

			if (iNext == 21 && tile.tilRot == 0) iRes = 2;
			if (iNext == 21 && tile.tilRot == 180) iRes = 1;

			if (iNext == 48 && tile.tilRot == 90) iRes = 2;
			if (iNext == 48 && tile.tilRot == 270) iRes = 1;

			if (iNext == 84 && tile.tilRot == 90) iRes = 1;
			if (iNext == 84 && tile.tilRot == 270) iRes = 2;

			break;
		case 1: // elbow
			if (iNext == 12 && tile.tilRot == 0) iRes = 2;
			if (iNext == 12 && tile.tilRot == 270) iRes = 1;

			if (iNext == 21 && tile.tilRot == 90) iRes = 1;
			if (iNext == 21 && tile.tilRot == 180) iRes = 2;

			if (iNext == 48 && tile.tilRot == 180) iRes = 1;
			if (iNext == 48 && tile.tilRot == 270) iRes = 2;

			if (iNext == 84 && tile.tilRot == 0) iRes = 1;
			if (iNext == 84 && tile.tilRot == 90) iRes = 2;

			break;
		case 2: // T
			if (iNext == 12 && tile.tilRot == 0) iRes = 1;
			if (iNext == 12 && tile.tilRot == 180) iRes = 2;
			if (iNext == 12 && tile.tilRot == 270) iRes = 3;

			if (iNext == 21 && tile.tilRot == 0) iRes = 2;
			if (iNext == 21 && tile.tilRot == 90) iRes = 3;
			if (iNext == 21 && tile.tilRot == 180) iRes = 1;

			if (iNext == 48 && tile.tilRot == 90) iRes = 2;
			if (iNext == 48 && tile.tilRot == 180) iRes = 3;
			if (iNext == 48 && tile.tilRot == 270) iRes = 1;

			if (iNext == 84 && tile.tilRot == 0) iRes = 3;
			if (iNext == 84 && tile.tilRot == 90) iRes = 1;
			if (iNext == 84 && tile.tilRot == 270) iRes = 2;

			break;
		case 3: // +
			if (iNext == 12 && tile.tilRot == 0) iRes = 1;
			if (iNext == 12 && tile.tilRot == 90) iRes = 4;
			if (iNext == 12 && tile.tilRot == 180) iRes = 2;
			if (iNext == 12 && tile.tilRot == 270) iRes = 3;

			if (iNext == 21 && tile.tilRot == 0) iRes = 2;
			if (iNext == 21 && tile.tilRot == 90) iRes = 3;
			if (iNext == 21 && tile.tilRot == 180) iRes = 1;
			if (iNext == 21 && tile.tilRot == 270) iRes = 4;

			if (iNext == 48 && tile.tilRot == 0) iRes = 4;
			if (iNext == 48 && tile.tilRot == 90) iRes = 2;
			if (iNext == 48 && tile.tilRot == 180) iRes = 3;
			if (iNext == 48 && tile.tilRot == 270) iRes = 1;

			if (iNext == 84 && tile.tilRot == 0) iRes = 3;
			if (iNext == 84 && tile.tilRot == 90) iRes = 1;
			if (iNext == 84 && tile.tilRot == 180) iRes = 4;
			if (iNext == 84 && tile.tilRot == 270) iRes = 2;

			break;
		case 4: // end
			if (iNext == 12 && tile.tilRot == 0) iRes = 1;
			if (iNext == 12 && tile.tilRot == 180) iRes = 2;

			if (iNext == 21 && tile.tilRot == 0) iRes = 2;
			if (iNext == 21 && tile.tilRot == 180) iRes = 1;

			if (iNext == 48 && tile.tilRot == 90) iRes = 2;
			if (iNext == 48 && tile.tilRot == 270) iRes = 1;

			if (iNext == 84 && tile.tilRot == 90) iRes = 1;
			if (iNext == 84 && tile.tilRot == 270) iRes = 2;

			break;
		}

		return iRes;
	}

	// random rotate the tiles
	public void rotateTiles()
	{
		int rot = 0;
		for (PiperClass.Tile tile : tiles)
		{
			if (tile.fix == false)
			{
				if (getPipeType(tile.type) == pipeLine) rot = random(0, 1, 1);
				else if (getPipeType(tile.type) == pipeX) rot = 0;
				else rot = random(0, 3, 1);
				if (random(1, 10, 1) < difficulty) rot = 0;
				if (rot != 0)
				{
					rot *= 90;
					tile.desRot = tile.tilRot + rot;
					rotTile(tile);
				}
			}
		}
		return;
	}

	// generate a random integer
	public int random(int nMinimum, int nMaximum, int nRoundToInterval)
	{
		if (nMinimum > nMaximum)
		{
			int nTemp = nMinimum;
			nMinimum = nMaximum;
			nMaximum = nTemp;
		}

		int nDeltaRange = (nMaximum - nMinimum) + (1 * nRoundToInterval);
		double nRandomNumber = Math.random() * nDeltaRange;

		nRandomNumber += nMinimum;

		int nRet = (int) (Math.floor(nRandomNumber / nRoundToInterval) * nRoundToInterval);

		return nRet;
	}

	// PIPE LINE
	public void newTitle()
	{
		ready = false;
		
		for (int i = 0; i < this.mTableWidth; i++)
			for (int j = 0; j < this.mTableHeight; j++)
				tiles.add(addTile(i, j, 0, 0, true));
		
		int x = 0;
		int y = 0;
		// P
		modTile(x + 1, y + 1, 1, 180, true);
		modTile(x + 2, y + 1, 1, 270, true);
		modTile(x + 1, y + 2, 7, 90, true);
		modTile(x + 2, y + 2, 1, 0, true);
		modTile(x + 1, y + 3, 11, 90, true);
		// I
		modTile(x + 3, y + 1, 11, 270, true);
		modTile(x + 3, y + 2, 5, 270, true);
		modTile(x + 3, y + 3, 11, 90, true);
		// P
		modTile(x + 4, y + 1, 1, 180, true);
		modTile(x + 5, y + 1, 1, 270, true);
		modTile(x + 4, y + 2, 7, 90, true);
		modTile(x + 5, y + 2, 1, 0, true);
		modTile(x + 4, y + 3, 11, 90, true);
		// E
		modTile(x + 6, y + 1, 1, 180, true);
		modTile(x + 7, y + 1, 11, 0, true);
		modTile(x + 6, y + 2, 7, 90, true);
		modTile(x + 7, y + 2, 11, 0, true);
		modTile(x + 6, y + 3, 1, 90, true);
		modTile(x + 7, y + 3, 11, 0, true);
		// L
		modTile(x + 1, y + 5, 11, 270, true);
		modTile(x + 1, y + 6, 5, 90, true);
		modTile(x + 1, y + 7, 1, 90, true);
		modTile(x + 2, y + 7, 11, 0, true);
		// I
		modTile(x + 3, y + 5, 11, 270, true);
		modTile(x + 3, y + 6, 5, 270, true);
		modTile(x + 3, y + 7, 11, 90, true);
		// N
		modTile(x + 4, y + 5, 1, 180, true);
		modTile(x + 5, y + 5, 1, 270, true);
		modTile(x + 4, y + 6, 5, 90, true);
		modTile(x + 5, y + 6, 5, 90, true);
		modTile(x + 4, y + 7, 11, 90, true);
		modTile(x + 5, y + 7, 11, 90, true);
		// E
		modTile(x + 6, y + 5, 1, 180, true);
		modTile(x + 7, y + 5, 11, 0, true);
		modTile(x + 6, y + 6, 7, 90, true);
		modTile(x + 7, y + 6, 11, 0, true);
		modTile(x + 6, y + 7, 1, 90, true);
		modTile(x + 7, y + 7, 11, 0, true);

		ready = true;

		return;
	}

	// test tiles
	public void testWay()
	{
		for (int i = 0; i < this.mTableWidth; i++)
			for (int j = 0; j < this.mTableHeight; j++)
				tiles.add(addTile(i, j, 0, 0, true));

		modTile(1, 0, 11, 270, true);
		modTile(0, 1, 12, 180, true);
		modTile(2, 1, 14, 0, true);
		modTile(1, 2, 13, 90, true);

		// modTile(1, 1, 5, 0, true);// I
		// modTile(1, 1, 5, 90, true);// I
		// modTile(1, 1, 5, 180, true);// I
		// modTile(1, 1, 5, 270, true);// I

		// modTile(1, 1, 1, 0, true);// L
		// modTile(1, 1, 1, 90, true);// L
		// modTile(1, 1, 1, 180, true);// L
		// modTile(1, 1, 1, 270, true);// L

		// modTile(1, 1, 7, 0, true);// T
		// modTile(1, 1, 7, 90, true);// T
		// modTile(1, 1, 7, 180, true);// T
		// modTile(1, 1, 7, 270, true);// T

		// modTile(1, 1, 15, 0, true);// +
		// modTile(1, 1, 15, 90, true);// +
		// modTile(1, 1, 15, 180, true);// +
		modTile(1, 1, 15, 270, true);// +

		// startCell(0, 1, 4);
		// startCell(1, 0, 4);
		// startCell(2, 1, 4);
		startCell(1, 2, 4);
	}

	private boolean checkFullPipes()
	{
		int myWay, toWay;
		int x, y, idx;
		Tile nextTile;

		for (Tile tile : tiles)
		{
			x = (int) (tile.tilPosX / miCellWidth);
			y = (int) (tile.tilPosY / miCellHeight);
			myWay = getWay(tile);

			if (tile.fluidPercent >= 1)
			{
				// to left
				if ((myWay & wayLeft) == wayLeft)
				{
					if (x > 0)
					{
						idx = (x - 1) * this.mTableHeight + y;
						nextTile = tiles.get(idx);
						toWay = getWay(nextTile);
						if ((toWay & wayRight) != wayRight) return false;
						else if (nextTile.fluidPercent < 1) return false;
					}
					else return false;
				}
				// to right
				if ((myWay & wayRight) == wayRight)
				{
					if (x < mTableWidth - 1)
					{
						idx = (x + 1) * this.mTableHeight + y;
						nextTile = tiles.get(idx);
						toWay = getWay(nextTile);
						if ((toWay & wayLeft) != wayLeft) return false;
						else if (nextTile.fluidPercent < 1) return false;
					}
					else return false;
				}
				// to up
				if ((myWay & wayUp) == wayUp)
				{
					if (y > 0)
					{
						idx = x * this.mTableHeight + (y - 1);
						nextTile = tiles.get(idx);
						toWay = getWay(nextTile);
						if ((toWay & wayDown) != wayDown) return false;
						else if (nextTile.fluidPercent < 1) return false;
					}
					else return false;
				}
				// to down
				if ((myWay & wayDown) == wayDown)
				{
					if (y < mTableHeight - 1)
					{
						idx = x * this.mTableHeight + (y + 1);
						nextTile = tiles.get(idx);
						toWay = getWay(nextTile);
						if ((toWay & wayUp) != wayUp) return false;
						else if (nextTile.fluidPercent < 1) return false;
					}
					else return false;
				}
			}
		}

		return true;
	}
	
	private boolean checkAllPipes()
	{
		int myWay, toWay;
		int x, y, idx;
		Tile nextTile;

		for (Tile tile : tiles)
		{
			x = (int) (tile.tilPosX / miCellWidth);
			y = (int) (tile.tilPosY / miCellHeight);
			myWay = getWay(tile);

			// to left
			if ((myWay & wayLeft) == wayLeft)
			{
				if (x > 0)
				{
					idx = (x - 1) * this.mTableHeight + y;
					nextTile = tiles.get(idx);
					toWay = getWay(nextTile);
					if ((toWay & wayRight) != wayRight) return false;
				}
				else return false;
			}
			// to right
			if ((myWay & wayRight) == wayRight)
			{
				if (x < mTableWidth - 1)
				{
					idx = (x + 1) * this.mTableHeight + y;
					nextTile = tiles.get(idx);
					toWay = getWay(nextTile);
					if ((toWay & wayLeft) != wayLeft) return false;
				}
				else return false;
			}
			// to up
			if ((myWay & wayUp) == wayUp)
			{
				if (y > 0)
				{
					idx = x * this.mTableHeight + (y - 1);
					nextTile = tiles.get(idx);
					toWay = getWay(nextTile);
					if ((toWay & wayDown) != wayDown) return false;
				}
				else return false;
			}
			// to down
			if ((myWay & wayDown) == wayDown)
			{
				if (y < mTableHeight - 1)
				{
					idx = x * this.mTableHeight + (y + 1);
					nextTile = tiles.get(idx);
					toWay = getWay(nextTile);
					if ((toWay & wayUp) != wayUp) return false;
				}
				else return false;
			}
		}

		return true;
	}
	

	public int getEndGame()
	{
		return endGame;
	}

}
