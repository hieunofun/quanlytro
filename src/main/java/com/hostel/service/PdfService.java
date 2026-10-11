package com.hostel.service;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
@Service
public class PdfService {
    public byte[] render(String title,List<String> paragraphs) throws IOException {
        try(var doc=new PDDocument();var fontStream=getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")) {
            var font=PDType0Font.load(doc,Objects.requireNonNull(fontStream));List<String> lines=new ArrayList<>();lines.add(title);lines.add("");
            for(String paragraph:paragraphs) {
                for(String source:paragraph.split("\\R",-1)) {
                    source=source.replace('−','-').replace("→","->");
                    StringBuilder supported=new StringBuilder();
                    for(int codePoint:source.codePoints().toArray()) {
                        String glyph=new String(Character.toChars(codePoint));
                        try {font.encode(glyph);supported.append(glyph);} catch(IllegalArgumentException e) {supported.append('?');}
                    }
                    source=supported.toString();
                    StringBuilder line=new StringBuilder();
                    for(String word:source.replaceAll("[\\p{Cntrl}]"," ").split(" ")) {
                        // Split very long tokens as well as wrapping words.
                        while(font.getStringWidth(word)/1000*11>490) {
                            int cut=Math.min(55,word.length());lines.add(word.substring(0,cut));word=word.substring(cut);
                        }
                        if(font.getStringWidth(line+" "+word)/1000*11>490) {lines.add(line.toString());line.setLength(0);}
                        if(!line.isEmpty())line.append(' ');line.append(word);
                    }
                    lines.add(line.toString());
                }
            }
            for(int offset=0;offset<lines.size();offset+=43) {
                var page=new PDPage(PDRectangle.A4);doc.addPage(page);
                try(var stream=new PDPageContentStream(doc,page)) {
                    stream.beginText();stream.setFont(font,11);stream.setLeading(17);stream.newLineAtOffset(50,780);
                    for(int i=offset;i<Math.min(offset+43,lines.size());i++) {stream.showText(lines.get(i));stream.newLine();}stream.endText();
                }
            }
            var output=new ByteArrayOutputStream();doc.save(output);return output.toByteArray();
        }
    }
}
