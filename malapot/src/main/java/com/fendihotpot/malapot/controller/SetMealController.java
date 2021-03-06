package com.fendihotpot.malapot.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ResourceUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fendihotpot.malapot.dao.SetMealDAO;
import com.fendihotpot.malapot.domain.I9sBean;
import com.fendihotpot.malapot.domain.SetMealBean;
import com.fendihotpot.malapot.service.I9sService;
import com.fendihotpot.malapot.service.SetMealService;
import com.fendihotpot.malapot.service.TypeService;

@Controller
public class SetMealController {
	
	@PersistenceContext
	private Session session;

	@Autowired
	private SetMealService setMealService;
		
	@Autowired
	private SetMealDAO setMealDAO;
	
	@Autowired
	private TypeService typeService;
	

	// (??????)??????????????????/???????????????(meal.jsp)
	@RequestMapping(path = { "/backend/selectAllMeal.controller" })
	public String selectAll(Model model) {
		List<SetMealBean> test = setMealService.select(null);
		List<Set<I9sBean>> test1 = new ArrayList<Set<I9sBean>>();
		for(SetMealBean setMealBean: test) {
			Set<I9sBean> i9sBeans = setMealBean.getI9sBeans();
			test1.add(i9sBeans);
		}
		
		model.addAttribute("i9sBeanList",test1);
		model.addAttribute("setMeal",test);
		return "/backend/meal";
	}
	
	// (??????)??????update??????????????????????????????????????????????????????????????????
	@RequestMapping(path= {"/pages/selectThis/{id}"})
	public String selectThis(Model model,@PathVariable Integer id) {
		SetMealBean setMealBean = setMealDAO.select(id);
		model.addAttribute("setMealBean",setMealBean);
		return "/backend/mealupdate";
	}
	
	// ??????????????????+?????????
	@ModelAttribute
	public void addimage(Model model,HttpSession session) {
		String path = session.getServletContext().getRealPath("/upload/picture");
			model.addAttribute("path",path);
	}

	
	// (??????)???????????????????????????????????????????????????????????????????????????????????????
	@PostMapping(path= {"/pages/addMeal"})
	@ResponseBody
	public SetMealBean addMeal(SetMealBean bean,MultipartFile multipart,Model model){
		String path = (String)model.getAttribute("path");
		String fileName = UUID.randomUUID().toString() + multipart.getOriginalFilename().substring(multipart.getOriginalFilename().lastIndexOf('.'));
		bean.setPicture(fileName);
		try {
			multipart.transferTo(new File(path,fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
		SetMealBean insertMeal = setMealService.insertMeal(bean);
		return insertMeal;
	}
	
	// (??????)??????del????????????????????????????????????????????????????????????
	@RequestMapping(path= {"/pages/deleteMeal"})
	@ResponseBody
	public boolean deleteMeal(Integer id) {
		boolean deleteMeal = setMealService.deleteMeal(id);
		return deleteMeal;
	}
	
	// (??????)??????"????????????"????????????????????????
	@RequestMapping(path= {"/pages/updateMeal"})
	@ResponseBody
	public SetMealBean updateMeal(SetMealBean bean) {
		SetMealBean updateMeal = setMealService.updateMeal(bean);
		return updateMeal;
	}
	
	// (??????)??????"????????????"???????????????????????????+type1????????????
	@RequestMapping(path= {"/pages/updateList/{id}"})
	public String updateList(@PathVariable Integer id,Model model) {
		//??????this???????????????
		SetMealBean setMealBean = session.get(SetMealBean.class, id);
		Set<I9sBean> i9sBeans = setMealBean.getI9sBeans();
		//??????type1???????????????
		List<Set<?>> selectAll = typeService.selectAll();
		Set<?> set = selectAll.get(0);
		//type1???this????????????
		
		List<I9sBean> mealList = new ArrayList<I9sBean>();
		List<I9sBean> type1 = new ArrayList<I9sBean>();
		List<I9sBean> type1cutmealList = new ArrayList<I9sBean>();
		
		for(I9sBean bean:(Set<I9sBean>)set) {
			type1.add(bean);
			type1cutmealList.add(bean);
		}
		
		for(I9sBean bean:i9sBeans) {
			mealList.add(bean);
			type1cutmealList.remove(bean);
		}

		model.addAttribute("setMealBean",setMealBean);
		model.addAttribute("mealList",mealList);
		model.addAttribute("type1",type1cutmealList);
		
		return "/backend/meali9sbean";
	}
	
	// (??????)????????????????????????????????????????????????
	@RequestMapping(path= {"/pages/addi9sformeal/{id}"})
	@ResponseBody
	public Integer addi9sformeal(@RequestBody Integer[] ids,@PathVariable Integer id) {
		
		SetMealBean setMealBean = session.get(SetMealBean.class, id);
		List<I9sBean> i9sBeans = new ArrayList<I9sBean>();
		for(Integer i9sid:ids) {
			I9sBean i9sBean = session.get(I9sBean.class, i9sid);
			i9sBeans.add(i9sBean);
			setMealService.insertMealI9s(setMealBean, i9sBean);
		}	
		return i9sBeans.size();
	}
	
	//(??????)?????????????????????????????????????????????????????????
	@RequestMapping(path= {"/pages/deletei9sformeal/{id}"})
	@ResponseBody
	public Integer deletei9sformeal(@RequestBody Integer[] ids,@PathVariable Integer id) {
		SetMealBean setMealBean = session.get(SetMealBean.class, id);
		List<I9sBean> i9sBeans = new ArrayList<I9sBean>();
		for(Integer i9sid:ids) {
			I9sBean i9sBean = session.get(I9sBean.class, i9sid);
			i9sBeans.add(i9sBean);
			setMealService.deleteMealI9s(setMealBean, i9sBean);
		}
		return i9sBeans.size();
	}
	
}
