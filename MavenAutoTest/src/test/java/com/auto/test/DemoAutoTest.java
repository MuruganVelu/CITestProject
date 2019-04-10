package com.auto.test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.Test;

public class DemoAutoTest {
	
	public static WebDriver driver;

	@Test
	
	public void demoTest(){

	try{
		System.setProperty("webdriver.chrome.driver", "G:\\drivers\\chromedriver.exe");
		driver = new ChromeDriver();
		driver.get("http://demo.automationtesting.in/Register.html");
		driver.manage().window().maximize();
		WebElement firstname = driver.findElement(By.xpath("//div[contains(@class,'form-group')]//following-sibling::input[@placeholder='First Name']"));
		firstname.sendKeys("Murugan");
		Thread.sleep(3000);
		driver.quit();
	}catch(Exception e){
		e.printStackTrace();
	}
	}

}
