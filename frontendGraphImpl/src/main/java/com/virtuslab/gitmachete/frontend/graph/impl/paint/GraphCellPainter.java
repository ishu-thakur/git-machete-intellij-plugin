package com.virtuslab.gitmachete.frontend.graph.impl.paint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;

import javax.swing.JTable;

import io.vavr.collection.List;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.graph.api.paint.IColorProvider;
import com.virtuslab.gitmachete.frontend.graph.api.paint.IGraphCellPainter;
import com.virtuslab.gitmachete.frontend.graph.api.paint.PaintParameters;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IEdgeRenderPart;
import com.virtuslab.gitmachete.frontend.graph.api.render.parts.IRenderPart;

@RequiredArgsConstructor
public class GraphCellPainter implements IGraphCellPainter {

  private final IColorProvider colorProvider;

  private final JTable table;

  @UIEffect
  protected int getRowHeight() {
    return table.getRowHeight();
  }

  @UIEffect
  private BasicStroke getOrdinaryStroke() {
    return new BasicStroke(PaintParameters.getLineThickness(getRowHeight()), BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_BEVEL);
  }

  @UIEffect
  private void paintUpLine(Graphics2D g2, Color color, int posInRow) {
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int x = nodeWidth * posInRow + nodeWidth / 2;
    int y1 = getRowHeight() / 2 - 1;
    int y2 = 0;
    paintLine(g2, color, x, y1, x, y2);
  }

  @UIEffect
  private void paintDownLine(Graphics2D g2, Color color, int posInRow) {
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int y2 = getRowHeight();
    int y1 = getRowHeight() / 2;
    int x = nodeWidth * posInRow + nodeWidth / 2;
    paintLine(g2, color, x, y1, x, y2);
  }

  @UIEffect
  private void paintRightLine(Graphics2D g2, Color color, int posInRow) {
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int x1 = nodeWidth * posInRow + nodeWidth / 2;
    int x2 = x1 + nodeWidth;
    int y = getRowHeight() / 2;
    paintLine(g2, color, x1, y, x2, y);
  }

  @UIEffect
  private void paintLine(Graphics2D g2, Color color, int x1, int y1, int x2, int y2) {
    g2.setColor(color);
    g2.setStroke(getOrdinaryStroke());
    g2.drawLine(x1, y1, x2, y2);
  }

  @UIEffect
  private void paintCircle(Graphics2D g2, int position, Color color) {
    int nodeWidth = PaintParameters.getNodeWidth(getRowHeight());
    int circleRadius = PaintParameters.getCircleRadius(getRowHeight());

    int x0 = nodeWidth * position + nodeWidth / 2;
    int y0 = getRowHeight() / 2;
    int r = circleRadius;
    Ellipse2D.Double circle = new Ellipse2D.Double(x0 - r + 0.5, y0 - r + 0.5, 2 * r, 2 * r);
    g2.setColor(color);
    g2.fill(circle);
  }

  @UIEffect
  private Color getColor(IRenderPart renderPart) {
    return colorProvider.getColor(renderPart.getColorId());
  }

  @Override
  @UIEffect
  public void draw(Graphics2D g2, List<? extends IRenderPart> renderParts) {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    for (IRenderPart renderPart : renderParts) {
      drawRenderPart(g2, renderPart);
    }
  }

  @UIEffect
  protected void drawRenderPart(Graphics2D g2, IRenderPart renderPart) {
    if (renderPart.isNode()) {
      int posInRow = renderPart.getPositionInRow();
      paintCircle(g2, posInRow, getColor(renderPart));
    } else { // isEdge
      drawEdge(g2, getColor(renderPart), renderPart.asEdge());
    }
  }

  @UIEffect
  private void drawEdge(Graphics2D g2, Color color, IEdgeRenderPart edgeRenderPart) {
    int posInRow = edgeRenderPart.getPositionInRow();

    if (edgeRenderPart.getType() == IEdgeRenderPart.Type.DOWN) {
      paintDownLine(g2, color, posInRow);
    } else if (edgeRenderPart.getType() == IEdgeRenderPart.Type.UP) {
      paintUpLine(g2, color, posInRow);
    } else if (edgeRenderPart.getType() == IEdgeRenderPart.Type.RIGHT) {
      paintRightLine(g2, color, posInRow);
    }
  }
}
